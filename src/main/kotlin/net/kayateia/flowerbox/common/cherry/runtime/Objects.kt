/*
	Flowerbox
	Copyright 2018 Kayateia

	This code is licensed under the MIT license; please see LICENSE.md in the root of the project.
 */

package net.kayateia.flowerbox.common.cherry.runtime

import net.kayateia.flowerbox.common.cherry.parser.*
import net.kayateia.flowerbox.common.cherry.runtime.scope.Scope
import net.kayateia.flowerbox.common.cherry.runtime.step.Step
import net.kayateia.flowerbox.common.cherry.runtime.step.ast.CallExpr

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

class ClassValue(val ast: AstClassDecl, val capturedScope: Scope) : Value {
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
		if (ast.base != null) {
			val baseClass = runtime.library.lookup(ast.base, runtime.nsUsings)
			if (baseClass == null || baseClass !is ClassValue)
				throw Exception("can't locate base class of ${ast.name} (${ast.base})")

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
			val initValue = FuncValue(initMethod.func, obj, capturedScope).call(runtime, args)
			if (initValue is ThrownValue)
				return initValue
		}

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

	suspend fun get(runtime: Runtime, obj: ObjectValue?, name: String): Value {
		val scv = staticConstruct(runtime)
		if (scv is ThrownValue)
			return scv

		// Do we have a method with that name?
		val method = methods[name]
		if (method != null) {
			if ((obj == null) != method.node.static)
				throw Exception("can't call static method on non-static, and vice-versa")
			return FuncValue(method.func, obj, capturedScope)
		}

		val map = obj?.map ?: companion

		// Do we have a field with that name?
		val field = fields[name]
		if (field != null) {
			if ((obj == null) != field.static)
				throw Exception("can't get static field on non-static, and vice-versa")
			return map[name] ?: NullValue()
		}

		// Look for a getter.
		val getter = getters[name]
		if (getter != null) {
			if ((obj == null) != getter.node.static)
				throw Exception("can't call static getter on non-static, and vice-versa")
			return Value.root(runtime, FuncValue(getter.func, obj, capturedScope).call(runtime, ListValue()))
		}

		throw Exception("invalid member read access for $name")
	}

	suspend fun put(runtime: Runtime, obj: ObjectValue?, name: String, value: Value): Value {
		val scv = staticConstruct(runtime)
		if (scv is ThrownValue)
			return scv

		val map = obj?.map ?: companion

		// Can't put methods.
		val method = methods[name]
		if (method != null)
			throw Exception("can't set over a method")

		// Do we have a field with that name?
		val field = fields[name]
		if (field != null) {
			if ((obj == null) != field.static)
				throw Exception("can't set static field on non-static, and vice-versa")
			map[name] = value
			return NullValue()
		} else {
			// Look for a setter.
			val setter = setters[name]
			if (setter != null) {
				if ((obj == null) != setter.node.static)
					throw Exception("can't call static setter on non-static, and vice-versa")
				return Value.root(runtime, FuncValue(setter.func, obj, capturedScope).call(runtime, ListValue(mutableListOf(value))))
			} else
				throw Exception("invalid member write access for $name")
		}
	}
}

class ObjectAccessor(val obj: ObjectValue, val name: String) : LValue, RValue {
	override suspend fun read(runtime: Runtime): Any? = obj.`class`.get(runtime, obj, name)
	override suspend fun write(runtime: Runtime, value: Any?) {
		obj.`class`.put(runtime, obj, name, Value.box(value))
	}
}

class ObjectValue(val `class`: ClassValue) : Value {
	val map = HashMap<String, Value>()

	override fun toString(): String = "Object(${`class`.ast.name}: ${map.entries.fold("", {a, b -> "$a,${b.key}:${b.value}"})})"
}
