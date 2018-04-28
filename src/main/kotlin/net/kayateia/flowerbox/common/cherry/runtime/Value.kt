/*
	Flowerbox
	Copyright 2018 Kayateia

	This code is licensed under the MIT license; please see LICENSE.md in the root of the project.
 */

package net.kayateia.flowerbox.common.cherry.runtime

import net.kayateia.flowerbox.common.cherry.parser.AstFuncExpr
import net.kayateia.flowerbox.common.cherry.runtime.scope.Scope

interface Value {
	var value: Any?
	val rvalue: RValue
		get() = RValue(value)

	companion object {
		fun getRootValue(value: Any?): Any? = when (value) {
			is Value -> getRootValue(value.value)
			else -> value
		}
	}
}

interface LValue : Value

class ScopeLValue(val scope: Scope, val name: String) : LValue {
	override var value: Any?
		get() = scope.get(name)
		set(value) {
			scope.set(name, when (value) {
				is Value -> value.rvalue
				else -> RValue(value)
			})
		}

	override fun toString(): String = "LValue(${name})"
}

class FuncValue(val funcNode: AstFuncExpr, val capturedScope: Scope) : Value {
	override var value: Any?
		get() = throw Exception("can't use function value as RValue")
		set(value) {
			throw Exception("can't assign into a function value")
		}

	override fun toString(): String = "FuncValue(${funcNode.id}(${funcNode.params.fold("", {a,b -> "$a,$b"})})"
}

class IntrinsicValue(val delegate: (runtime: Runtime, implicits: Scope, args: ArrayValue) -> Value) : Value {
	override var value: Any?
		get() = throw Exception("can't use intrinsic as RValue")
		set(value) {
			throw Exception("can't assign into an intrinsic")
		}

	override fun toString(): String = "${delegate.javaClass.name}()"
}

class RValue(val constValue: Any?) : Value {
	override var value: Any?
		get() = constValue
		set(value) {
			throw Exception("can't set a constant (RValue)")
		}
	override fun toString(): String = "RValue(${value})"
}

class NullValue : Value {
	override var value: Any?
		get() = null
		set(value) {
			throw Exception("can't assign into a null value")
		}

	override fun toString(): String = "RValue(null)"
}

class ArrayValue(val arrayValue: List<Value>) : Value {
	override var value: Any?
		get() = throw Exception("can't use array value as RValue")
		set(value) {
			throw Exception("can't assign into an array value")
		}

	override fun toString(): String = "ArrayValue(${arrayValue.fold("", {a,b -> "$a,$b"})})"
}

class ObjectSetter(val obj: ObjectValue, val key: String) :  LValue {
	override var value: Any?
		get() = obj.read(key)
		set(value) {
			if (value is Value)
				obj.write(key, value)
			else
				obj.write(key, RValue(value))
		}
}

open class ObjectValue(val map: HashMap<String, Value>, val readOnly: Boolean) : Value {
	override var value: Any?
		get() = throw Exception("can't use object value as RValue")
		set(value) {
			throw Exception("can't assign into an object value")
		}

	fun write(key: String, value: Value) = map.put(key, value)
	fun read(key: String): Value? =
		if (readOnly)
			map[key]?.rvalue
		else
			ObjectSetter(this, key)
	fun has(key: String) = map.containsKey(key)

	override fun toString(): String = "ObjectValue(${map.entries.fold("", {a, b -> "$a,${b.key}:${b.value}"})})"
}

// A static is basically a special variant of a read-only object with an associated name and namespace.
// Only the compiler can create a static, so it should be distinct from just objects.
// Name should not include the static, and namespace should be formatted.with.dots.
interface StaticObjectValue {
	val name: String
}

class NamespaceValue(override val name: String) : StaticObjectValue, ObjectValue(HashMap(), true)
class ClassValue(val namespace: String, override val name: String, val base: ClassValue) : StaticObjectValue, ObjectValue(HashMap(), true)

interface FlowControlValue : Value

class ReturnValue(val returnValue: Value) : FlowControlValue {
	override var value: Any?
		get() = returnValue
		set(value) {
			throw Exception("can't assign into a return value")
		}
}

class BreakValue : FlowControlValue {
	override var value: Any?
		get() = throw Exception("can't get the value of a break")
		set(value) {
			throw Exception("can't assign into a break value")
		}
}

class ContinueValue : FlowControlValue {
	override var value: Any?
		get() = throw Exception("can't get the value of a continue")
		set(value) {
			throw Exception("can't assign into a continue value")
		}
}

class ThrownValue(val thrownValue: Value) : FlowControlValue {
	override var value: Any?
		get() = thrownValue
		set(value) {
			throw Exception("can't assign into a thrown value")
		}
}
