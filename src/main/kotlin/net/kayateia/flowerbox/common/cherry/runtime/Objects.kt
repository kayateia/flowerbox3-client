/*
	Flowerbox
	Copyright 2018 Kayateia

	This code is licensed under the MIT license; please see LICENSE.md in the root of the project.
 */

package net.kayateia.flowerbox.common.cherry.runtime

import net.kayateia.flowerbox.common.cherry.parser.*
import net.kayateia.flowerbox.common.cherry.runtime.scope.ConstScope
import net.kayateia.flowerbox.common.cherry.runtime.scope.Scope
import net.kayateia.flowerbox.common.cherry.runtime.step.Step

class NamespaceValue(val namespace: String) : Value {
	val map = HashMap<String, Value>()

	fun addOne(pkgList: List<String>, name: String, value: Value) {
		if (pkgList.isEmpty()) {
			map[name] = value
		} else {
			val cur = pkgList.first()
			var curns: NamespaceValue? = map[cur] as NamespaceValue?
			if (curns == null) {
				// TODO - Should actually check ns.has(cur) here in case there's a class already.
				curns = NamespaceValue(cur)
				map[cur] = curns
			}
			curns.addOne(pkgList.drop(1), name, value)
		}
	}

	override fun toString(): String = "NamespaceValue(${map.entries.fold("", {a, b -> "$a,${b.key}:${b.value}"})})"
}

class ClassAccessor(val `class`: ClassValue, val name: String) : LValue, RValue {
	override suspend fun read(runtime: Runtime): Any? = `class`.get(runtime, null, name)
	override suspend fun write(runtime: Runtime, value: Any?) {
		`class`.put(runtime, null, name, Value.box(value))
	}
}

data class FieldValue(val node: AstFieldDecl, val scope: AstScopeType, val static: Boolean, val init: AstExpr?)
data class MethodValue(val func: AstFuncExpr, val node: AstMethodDecl)
data class AccessorValue(val func: AstFuncExpr, val node: AstAccessorDecl)

class ClassValue(val ast: AstClassDecl, val namespace: String, val capturedScope: Scope, val capturedUsings: List<String>) : Value {
	private val fields = HashMap<String, FieldValue>()
	private val methods = HashMap<String, MethodValue>()
	private val getters = HashMap<String, AccessorValue>()
	private val setters = HashMap<String, AccessorValue>()

	// Companion object representing class statics.
	private val companion = HashMap<String, Value>()
	private var staticConstructComplete = false

	init {
		// Split out the unprocessed body decls into separate buckets for easier finding.
		for (i in ast.body) {
			when (i) {
				is AstFieldDecl -> i.decls.forEach { fields[it.id] = FieldValue(i, i.scope, i.static, it.init) }
				is AstMethodDecl -> methods[i.body.func.id!!] = MethodValue(i.body.func, i)	// Need !! here because of overloading the AstFuncExpr class.
				is AstAccessorDecl -> when (i.type) {
					AstAccessorType.GET -> getters[i.name] = AccessorValue(AstFuncExpr(i.loc, i.name, null, AstBlock(i.loc, i.body)), i)
					AstAccessorType.SET -> setters[i.name] = AccessorValue(AstFuncExpr(i.loc, i.name, listOf(i.arg!!), AstBlock(i.loc, i.body)), i)	// TODO - check i.arg
				}
			}
		}
	}

	private suspend fun staticConstruct(runtime: Runtime): Value {
		if (staticConstructComplete)
			return NullValue()

		// Do static initializers.
		for (f in fields) {
			if (f.value.static && f.value.init != null) {
				val initValue = Value.root(runtime, Step.exec(runtime, f.value.init!!))
				companion[f.key] = initValue
			}
		}

		staticConstructComplete = true
		return NullValue()
	}

	suspend fun construct(runtime: Runtime, args: ListValue): Value {
		// Make the object.
		val obj = ObjectValue(this)

		// First, do any implicit constructor work from initializers in base classes.
		val baseClass = getBaseClass(runtime)
		if (baseClass != null) {
			val baseConstruct = baseClass.implicitConstruct(runtime, obj)
			if (baseConstruct is ThrownValue)
				return baseConstruct
		}

		// Next, do local implicit constructor work.
		val usConstruct = implicitConstruct(runtime, obj)
		if (usConstruct is ThrownValue)
			return usConstruct

		// Finally, look for any actual constructor method and call it if needed.
		val initMethod = methods["init"]
		if (initMethod != null) {
			val funcScope = getMethodScope(obj, baseClass)
			val initValue = FuncValue(initMethod.func, funcScope).call(runtime, args)
			if (initValue is ThrownValue)
				return initValue
		} else if (baseClass != null) {
			// If this object had no constructor, the base has no chance to run. So we need
			// to provide an implicit default constructor that calls the base.
			val baseInitMethod = baseClass.getMethod(runtime, ObjectValue(baseClass, obj.map), "init")
			if (baseInitMethod != null && baseInitMethod is FuncValue)
				baseInitMethod.call(runtime, ListValue())
		}

		// TODO - Force any constructor to include a call to the base constructor?

		// No explicit constructor - just return the object.
		return obj
	}

	private suspend fun implicitConstruct(runtime: Runtime, obj: ObjectValue): Value {
		val scv = staticConstruct(runtime)
		if (scv is ThrownValue)
			return scv

		for (f in fields) {
			val init = f.value.init
			if (init != null) {
				val initValue = Value.root(runtime, Step.exec(runtime, init))
				if (initValue is ThrownValue)
					return initValue
				obj.map[f.key] = initValue
			}
		}

		return NullValue()
	}

	private fun getBaseClass(runtime: Runtime): ClassValue? {
		if (ast.base != null) {
			val baseClass = runtime.library.lookup(ast.base, capturedUsings)
			if (baseClass == null || baseClass !is ClassValue) {
				throw Exception("can't subclass from non-class or empty value $baseClass (${ast.name}:${ast.base})")
			}
			return baseClass
		}
		return null
	}

	private fun getMethodScope(obj: ObjectValue?, baseClass: ClassValue?): Scope {
		val funcScope = ConstScope(capturedScope)
		funcScope.setConstant("self", obj ?: NullValue())

		// This is the same object contents, it just redirects to the base class for class ops.
		if (obj != null && baseClass != null) {
			val baseObj = ObjectValue(baseClass, obj.map)
			funcScope.setConstant("base", baseObj)
		} else
			funcScope.setConstant("base", NullValue())

		return funcScope
	}

	private fun getMethod(runtime: Runtime, obj: ObjectValue?, name: String): Value? {
		val baseClass = getBaseClass(runtime)

		// Do we have a method with that name?
		val method = methods[name]
		if (method != null) {
			if ((obj == null) != method.node.static)
				throw Exception("can't call static method on non-static, and vice-versa")

			val funcScope = getMethodScope(obj, baseClass)

			// Is it a native method?
			if (method.node.native) {
				// See if we have it.
				val nativeImpl = runtime.nativeLibrary.namespaces[namespace]?.map?.get(ast.name)?.map?.get(name)
					?: throw Exception("no implementation for native method $namespace.${ast.name}.$name")

				return NativeValue(nativeImpl, funcScope)
			}

			return FuncValue(method.func, funcScope)
		}

		// Try base classes.
		if (baseClass != null)
			return baseClass.getMethod(runtime, obj, name)
		else
			return null
	}

	private fun getField(runtime: Runtime, obj: ObjectValue?, objMap: HashMap<String, Value>, name: String): Value? {
		// Do we have a field with that name?
		val field = fields[name]
		if (field != null) {
			if ((obj == null) != field.static)
				throw Exception("can't get static field on non-static, and vice-versa")
			return objMap[name] ?: NullValue()
		}

		// Try base classes.
		val baseClass = getBaseClass(runtime)
		if (baseClass != null)
			return baseClass.getField(runtime, obj, objMap, name)
		else
			return null
	}

	private suspend fun getByGetter(runtime: Runtime, obj: ObjectValue?, objMap: HashMap<String, Value>, name: String): Value? {
		val baseClass = getBaseClass(runtime)

		// Look for a getter.
		val getter = getters[name]
		if (getter != null) {
			if ((obj == null) != getter.node.static)
				throw Exception("can't call static getter on non-static, and vice-versa")

			val funcScope = getMethodScope(obj, baseClass)

			// Is it a native method?
			if (getter.node.native) {
				// See if we have it.
				val nativeImpl = runtime.nativeLibrary.namespaces[namespace]?.map?.get(ast.name)?.map?.get(name)
						?: throw Exception("no implementation for native getter $namespace.${ast.name}.$name")

				return nativeImpl.impl(runtime, name, objMap, funcScope, ListValue())
			}

			return Value.root(runtime, FuncValue(getter.func, funcScope).call(runtime, ListValue()))
		}

		// Try base classes.
		if (baseClass != null)
			return baseClass.getByGetter(runtime, obj, objMap, name)
		else
			return null
	}

	suspend fun get(runtime: Runtime, obj: ObjectValue?, name: String): Value {
		val scv = staticConstruct(runtime)
		if (scv is ThrownValue)
			return scv

		val methodValue = getMethod(runtime, obj, name)
		if (methodValue != null)
			return methodValue

		val map = obj?.map ?: companion

		val fieldValue = getField(runtime, obj, map, name)
		if (fieldValue != null)
			return fieldValue

		// Look for a getter.
		val getterValue = getByGetter(runtime, obj, map, name)
		if (getterValue != null)
			return getterValue

		throw Exception("invalid member read access for $name")
	}

	private fun setField(runtime: Runtime, obj: ObjectValue?, objMap: HashMap<String, Value>, name: String, value: Value): Value? {
		// Do we have a field with that name?
		val field = fields[name]
		if (field != null) {
			if ((obj == null) != field.static)
				throw Exception("can't set static field on non-static, and vice-versa")
			objMap[name] = value
			return NullValue()
		}

		// Try base classes.
		val baseClass = getBaseClass(runtime)
		if (baseClass != null)
			return baseClass.getField(runtime, obj, objMap, name)
		else
			return null
	}

	private suspend fun setBySetter(runtime: Runtime, obj: ObjectValue?, objMap: HashMap<String, Value>, name: String, value: Value): Value? {
		val baseClass = getBaseClass(runtime)

		// Look for a setter.
		val setter = setters[name]
		if (setter != null) {
			if ((obj == null) != setter.node.static)
				throw Exception("can't call static getter on non-static, and vice-versa")

			val funcScope = getMethodScope(obj, baseClass)

			// Is it a native method?
			if (setter.node.native) {
				// See if we have it.
				val nativeImpl = runtime.nativeLibrary.namespaces[namespace]?.map?.get(ast.name)?.map?.get(name)
						?: throw Exception("no implementation for native setter $namespace.${ast.name}.$name")

				return nativeImpl.impl(runtime, name, objMap, funcScope, ListValue(mutableListOf(value)))
			}

			return Value.root(runtime, FuncValue(setter.func, funcScope).call(runtime, ListValue(mutableListOf(value))))
		}

		// Try base classes.
		if (baseClass != null)
			return baseClass.setBySetter(runtime, obj, objMap, name, value)
		else
			return null
	}

	suspend fun put(runtime: Runtime, obj: ObjectValue?, name: String, value: Value): Value {
		val scv = staticConstruct(runtime)
		if (scv is ThrownValue)
			return scv

		val map = obj?.map ?: companion

		// Can't put methods.
		val methodValue = getMethod(runtime, obj, name)
		if (methodValue != null)
			throw Exception("can't set over a method")

		// Do we have a field with that name?
		val fieldValue = setField(runtime, obj, map, name, value)
		if (fieldValue != null)
			return fieldValue

		val setterValue = setBySetter(runtime, obj, map, name, value)
		if (setterValue != null)
			return setterValue

		throw Exception("invalid member write access for $name")
	}
}

class ObjectAccessor(val obj: ObjectValue, val name: String) : LValue, RValue {
	override suspend fun read(runtime: Runtime): Any? = obj.`class`.get(runtime, obj, name)
	override suspend fun write(runtime: Runtime, value: Any?) {
		obj.`class`.put(runtime, obj, name, Value.box(value))
	}
}

class ObjectValue(val `class`: ClassValue, val map: HashMap<String, Value> = HashMap()) : Value {
	override fun toString(): String = "Object(${`class`.ast.name}: ${map.entries.fold("", {a, b -> "$a,${b.key}:${b.value}"})})"
}
