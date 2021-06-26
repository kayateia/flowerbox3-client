/*
	Flowerbox
	Copyright 2018 Kayateia

	Please see LICENSE.txt in the root of the project for more details.
 */

package net.kayateia.flowerbox.common.cherry.runtime

import net.kayateia.flowerbox.common.cherry.objects.CherryNative
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
	override suspend fun read(runtime: Runtime): Any? {
		val scv = `class`.staticConstruct(runtime)
		return if (scv is ThrownValue)
			scv
		else
			`class`.companionClass?.get(runtime, `class`.companionObject!!, name) ?: throw Exception("class ${`class`.ast.name} has no static companion class")
	}
	override suspend fun write(runtime: Runtime, value: Any?): Value {
		val scv = `class`.staticConstruct(runtime)
		return if (scv is ThrownValue)
			scv
		else {
			if (`class`.companionClass != null)
				`class`.companionClass.put(runtime, `class`.companionObject!!, name, Value.box(value))
			else
				throw Exception("class ${`class`.ast.name} has no static companion class")
		}
	}
}

data class FieldValue(val node: AstFieldDecl, val scope: AstScopeType, val static: Boolean, val init: AstExpr?)
data class MethodValue(val func: AstFuncExpr, val node: AstMethodDecl)
data class AccessorValue(val func: AstFuncExpr, val node: AstAccessorDecl)

object ClassSpecialNames {
	const val GetterPrefix = "get::"
	const val SetterPrefix = "set::"
	const val InitMethod = "init"
	const val NativeInitMethod = "native::init"
	const val NativeStaticInitMethod = "static::native::init"
}

class ClassValue(val ast: AstClassDecl, val namespace: String, val capturedScope: Scope, val capturedUsings: List<String>, val static: Boolean) : Value {
	private val fields = HashMap<String, FieldValue>()
	private val methods = HashMap<String, MethodValue>()
	private val getters = HashMap<String, AccessorValue>()
	private val setters = HashMap<String, AccessorValue>()

	// Companion object representing class statics. Static companion classes do not have companion classes or objects.
	val companionClass: ClassValue? = if (!static)
			ClassValue(ast, namespace, capturedScope, capturedUsings, true)
		else
			null
	var companionObject: ObjectValue? = null

	init {
		// Split out the unprocessed body decls into separate buckets for easier finding.
		// We will also discriminate out the static vs non-static to make a secondary, implicit class.
		for (i in ast.body) {
			if (static == i.static) {
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
	}

	suspend fun staticConstruct(runtime: Runtime): Value {
		if (companionClass == null || companionObject != null)
			return NullValue()

		val result = companionClass.construct(runtime, ListValue())
		return if (result !is ObjectValue)
			result
		else {
			companionObject = result
			NullValue()
		}
	}

	suspend fun construct(runtime: Runtime, args: ListValue): Value {
		val scv = staticConstruct(runtime)
		if (scv is ThrownValue)
			return scv

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

		// If we have a native constructor (which provides the native companion object) then
		// call that. TODO - What to do with subclasses of native objects? The probable answer
		// is that we will want to ensure that Cherry subclasses parallel Kotlin subclasses.
		val nativeInitName = if (static)
				ClassSpecialNames.NativeStaticInitMethod
			else
				ClassSpecialNames.NativeInitMethod
		val nativeInit = runtime.nativeLibrary.namespaces[namespace]?.map?.get(ast.name)?.map?.get(nativeInitName)
		if (nativeInit != null) {
			nativeInit.impl(runtime, ClassSpecialNames.NativeInitMethod, obj, capturedScope, ListValue(mutableListOf(obj)))
		}

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
			val baseInitMethod = baseClass.getMethod(runtime, ObjectValue(baseClass, obj.map), ClassSpecialNames.InitMethod)
			if (baseInitMethod != null && baseInitMethod is FuncValue)
				baseInitMethod.call(runtime, ListValue())
		}

		// TODO - Force any constructor to include a call to the base constructor?

		// No explicit constructor - just return the object.
		return obj
	}

	private suspend fun implicitConstruct(runtime: Runtime, obj: ObjectValue): Value {
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
			return if (static)
				baseClass.companionClass
			else
				baseClass
		}
		return null
	}

	private fun getMethodScope(obj: ObjectValue, baseClass: ClassValue?): Scope {
		val funcScope = ConstScope(capturedScope)
		funcScope.setConstant("self", obj)

		// This is the same object contents, it just redirects to the base class for class ops.
		if (baseClass != null) {
			val baseObj = ObjectValue(baseClass, obj.map)
			funcScope.setConstant("base", baseObj)
		} else
			funcScope.setConstant("base", NullValue())

		return funcScope
	}

	private fun getMethod(runtime: Runtime, obj: ObjectValue, name: String): Value? {
		val baseClass = getBaseClass(runtime)

		// Do we have a method with that name?
		val method = methods[name]
		if (method != null) {
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
		return if (baseClass != null)
			baseClass.getMethod(runtime, obj, name)
		else
			null
	}

	private fun getField(runtime: Runtime, obj: ObjectValue, name: String): Value? {
		// Do we have a field with that name?
		val field = fields[name]
		if (field != null) {
			return obj.map[name] ?: NullValue()
		}

		// Try base classes.
		val baseClass = getBaseClass(runtime)
		return if (baseClass != null)
			baseClass.getField(runtime, obj, name)
		else
			null
	}

	private suspend fun getByGetter(runtime: Runtime, obj: ObjectValue, name: String): Value? {
		val baseClass = getBaseClass(runtime)

		// Look for a getter.
		val getter = getters[name]
		if (getter != null) {
			val funcScope = getMethodScope(obj, baseClass)

			// Is it a native method?
			if (getter.node.native) {
				// See if we have it.
				val nativeImpl = runtime.nativeLibrary.namespaces[namespace]?.map?.get(ast.name)?.map?.get("${ClassSpecialNames.GetterPrefix}$name")
						?: throw Exception("no implementation for native getter $namespace.${ast.name}.$name (${ClassSpecialNames.GetterPrefix}$name)")

				return nativeImpl.impl(runtime, name, obj, funcScope, ListValue())
			}

			return Value.root(runtime, FuncValue(getter.func, funcScope).call(runtime, ListValue()))
		}

		// Try base classes.
		return if (baseClass != null)
			baseClass.getByGetter(runtime, obj, name)
		else
			null
	}

	suspend fun get(runtime: Runtime, obj: ObjectValue, name: String): Value {
		val methodValue = getMethod(runtime, obj, name)
		if (methodValue != null)
			return methodValue

		val fieldValue = getField(runtime, obj, name)
		if (fieldValue != null)
			return fieldValue

		// Look for a getter.
		val getterValue = getByGetter(runtime, obj, name)
		if (getterValue != null)
			return getterValue

		throw Exception("invalid member read access for $name")
	}

	private fun setField(runtime: Runtime, obj: ObjectValue, name: String, value: Value): Value? {
		// Do we have a field with that name?
		val field = fields[name]
		if (field != null) {
			obj.map[name] = value
			return NullValue()
		}

		// Try base classes.
		val baseClass = getBaseClass(runtime)
		return if (baseClass != null)
			baseClass.getField(runtime, obj, name)
		else
			null
	}

	private suspend fun setBySetter(runtime: Runtime, obj: ObjectValue, name: String, value: Value): Value? {
		val baseClass = getBaseClass(runtime)

		// Look for a setter.
		val setter = setters[name]
		if (setter != null) {
			if (static != setter.node.static)
				throw Exception("can't call static getter on non-static, and vice-versa")

			val funcScope = getMethodScope(obj, baseClass)

			// Is it a native method?
			if (setter.node.native) {
				// See if we have it.
				val nativeImpl = runtime.nativeLibrary.namespaces[namespace]?.map?.get(ast.name)?.map?.get("${ClassSpecialNames.SetterPrefix}$name")
						?: throw Exception("no implementation for native setter $namespace.${ast.name}.$name (${ClassSpecialNames.SetterPrefix}$name)")

				return nativeImpl.impl(runtime, name, obj, funcScope, ListValue(mutableListOf(value)))
			}

			return Value.root(runtime, FuncValue(setter.func, funcScope).call(runtime, ListValue(mutableListOf(value))))
		}

		// Try base classes.
		return if (baseClass != null)
			baseClass.setBySetter(runtime, obj, name, value)
		else
			null
	}

	suspend fun put(runtime: Runtime, obj: ObjectValue, name: String, value: Value): Value {
		val scv = staticConstruct(runtime)
		if (scv is ThrownValue)
			return scv

		// Can't put methods.
		val methodValue = getMethod(runtime, obj, name)
		if (methodValue != null)
			throw Exception("can't set over a method")

		// Do we have a field with that name?
		val fieldValue = setField(runtime, obj, name, value)
		if (fieldValue != null)
			return fieldValue

		val setterValue = setBySetter(runtime, obj, name, value)
		if (setterValue != null)
			return setterValue

		throw Exception("invalid member write access for $name")
	}
}

class ObjectAccessor(val obj: ObjectValue, val name: String) : LValue, RValue {
	override suspend fun read(runtime: Runtime): Any? = obj.`class`.get(runtime, obj, name)
	override suspend fun write(runtime: Runtime, value: Any?): Value = obj.`class`.put(runtime, obj, name, Value.box(value))
}

class ObjectValue(val `class`: ClassValue, val map: HashMap<String, Value> = HashMap(), var nativeObject: CherryNative? = null) : Value {
	override fun toString(): String = "Object(${`class`.ast.name}: ${map.entries.fold("", {a, b -> "$a,${b.key}:${b.value}"})})"
}
