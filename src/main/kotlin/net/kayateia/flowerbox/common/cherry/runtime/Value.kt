/*
	Flowerbox
	Copyright 2018 Kayateia

	This code is licensed under the MIT license; please see LICENSE.md in the root of the project.
 */

package net.kayateia.flowerbox.common.cherry.runtime

import net.kayateia.flowerbox.common.cherry.parser.AstFuncExpr
import net.kayateia.flowerbox.common.cherry.runtime.scope.Scope

// Represents any special (non-primitive) value in the runtime.
interface Value {
	companion object {
		// Takes any value, primitive/native or otherwise, and tries to return the primitive
		// value behind it. Throws if it's a non-readable non-primitive.
		fun prim(value: Any?): Any? = when (value) {
			is RValue -> prim(value.read())
			is Value -> throw Exception("can't read non-primitive Value $value")
			else -> value
		}

		// Converts this Value into something resembling a boolean.
		fun bool(value: Any?): Boolean = when (value) {
			is RValue -> Coercion.toBool(prim(value))
			is NullValue -> false
			is Value -> true
			else -> Coercion.toBool(value)
		}

		// Really naive implementation of value comparison.
		// TODO - non-Kotlin semantics and func/obj/etc identity comparisons
		fun compare(valueA: Any?, valueB: Any?): Boolean = prim(valueA) == prim(valueB)

		// Takes any Value and tries to return the simplest possible Value representing it.
		fun root(value: Value): Value = when (value) {
			is FlowControlValue -> value
			is RValue -> {
				val unboxed = value.read()
				if (unboxed is Value)
					root(unboxed)
				else
					value
			}
			else -> value
		}

		// Converts any value into a Value, taking care not to double-box.
		fun box(value: Any?): Value = when (value) {
			null -> NullValue()
			is Value -> value
			else -> ConstValue(value)
		}
	}
}

// Represents a readable non-primitive value in the runtime.
interface RValue : Value {
	fun read(): Any?
}

// Represents a writable non-primitive value in the runtime.
interface LValue : Value {
	fun write(value: Any?)
}

// A reference to an item in a scope which may be read or written.
class ScopeLValue(val scope: Scope, val name: String) : LValue, RValue {
	override fun read(): Any? = scope.get(name)
	override fun write(value: Any?) {
		scope.set(name, when (value) {
			is RValue -> value.read()
			is Value -> value
			else -> ConstValue(value)
		})
	}

	override fun toString(): String = "ScopeValue(${name})"
}

class FuncValue(val funcNode: AstFuncExpr, val capturedScope: Scope) : Value {
	override fun toString(): String = "FuncValue(${funcNode.id}(${funcNode.params?.fold("", {a,b -> "$a,$b"})}))"
}

class IntrinsicValue(val delegate: (runtime: Runtime, implicits: Scope, args: ListValue) -> Value) : Value {
	override fun toString(): String = "${delegate.javaClass.name}()"
}

class ConstValue(val constValue: Any?) : RValue {
	override fun read(): Any? = constValue
	override fun toString(): String = "ConstValue(${constValue})"
}

class NullValue : Value {
	override fun toString(): String = "NullValue()"
}

class ListSetter(val array: ListValue, val key: Int) : RValue, LValue {
	override fun read(): Any? = array.listValue[key]
	override fun write(value: Any?) {
		if (value is Value)
			array.listValue[key] = value
		else
			array.listValue[key] = ConstValue(value)
	}
}

class ListValue(val listValue: MutableList<Value>) : Value {
	override fun toString(): String = "List(${listValue.fold("", {a,b -> "$a,$b"})})"
}

class DictSetter(val obj: DictValue, val key: Any) : RValue, LValue {
	override fun read(): Any? = obj.map[key]

	override fun write(value: Any?) {
		if (value is Value)
			obj.write(key, value)
		else
			obj.write(key, ConstValue(value))
	}
}

class DictValue(val map: HashMap<Any, Value>, val readOnly: Boolean) : Value {
	fun write(key: Any, value: Value) = map.put(key, value)
	fun read(key: Any): Value? =
		if (readOnly)
			map[key]
		else
			DictSetter(this, key)
	fun has(key: String) = map.containsKey(key)

	override fun toString(): String = "DictValue(${map.entries.fold("", {a, b -> "$a,${b.key}:${b.value}"})})"
}

// A static object is basically a special variant of a read-only, string-keyed dictionary with
// an associated name and namespace. Only the compiler can create a static, so it should be
// distinct from just objects. Name should not include the static, and namespace should be
// formatted.with.dots.
open class ObjectValue(val name: String) : Value {
	val map = HashMap<String, Value>()

	fun write(key: String, value: Value) = map.put(key, value)
	fun read(key: String): Value? = map[key]	// TODO: getters and setters in classes
	fun has(key: String) = map.containsKey(key)

	override fun toString(): String = "ObjectValue(${map.entries.fold("", {a, b -> "$a,${b.key}:${b.value}"})})"
}

class NamespaceValue(val namespace: String) : ObjectValue(namespace)
class ClassValue(val namespace: String, val className: String, val base: ClassValue) : ObjectValue(namespace)

interface FlowControlValue : Value

class ReturnValue(val returnValue: Value) : FlowControlValue

class BreakValue : FlowControlValue

class ContinueValue : FlowControlValue

class ThrownValue(val thrownValue: Any?) : FlowControlValue, RValue {
	override fun read(): Any? = thrownValue
}
