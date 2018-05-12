/*
	Flowerbox
	Copyright 2018 Kayateia

	This code is licensed under the MIT license; please see LICENSE.md in the root of the project.
 */

package net.kayateia.flowerbox.common.cherry.runtime

import net.kayateia.flowerbox.common.cherry.runtime.scope.ConstScope
import net.kayateia.flowerbox.common.cherry.runtime.scope.Scope

// Represents any special (non-primitive) value in the runtime.
interface Value {
	companion object {
		// Takes any value, primitive/native or otherwise, and tries to return the primitive
		// value behind it. Throws if it's a non-readable non-primitive.
		suspend fun prim(runtime: Runtime, value: Any?): Any? = when (value) {
			is RValue -> prim(runtime, value.read(runtime))
			is Value -> throw Exception("can't read non-primitive Value $value")
			else -> value
		}

		// Converts this Value into something resembling a boolean.
		suspend fun bool(runtime: Runtime, value: Any?): Boolean = when (value) {
			is RValue -> Coercion.toBool(prim(runtime, value))
			is NullValue -> false
			is Value -> true
			else -> Coercion.toBool(value)
		}

		// Really naive implementation of value comparison.
		// TODO - non-Kotlin semantics and func/obj/etc identity comparisons
		suspend fun compare(runtime: Runtime, valueA: Any?, valueB: Any?): Boolean = prim(runtime, valueA) == prim(runtime, valueB)

		// Takes any Value and tries to return the simplest possible Value representing it.
		suspend fun root(runtime: Runtime, value: Value): Value = when (value) {
			is FlowControlValue -> value
			is RValue -> {
				val unboxed = value.read(runtime)
				if (unboxed is Value)
					root(runtime, unboxed)
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
	suspend fun read(runtime: Runtime): Any?
}

// Represents a writable non-primitive value in the runtime.
// This is allowed to return a Value for flow control results.
interface LValue : Value {
	suspend fun write(runtime: Runtime, value: Any?): Value
}

// A reference to an item in a scope which may be read or written.
class ScopeLValue(val scope: Scope, val name: String) : LValue, RValue {
	override suspend fun read(runtime: Runtime): Any? = scope.get(name)
	override suspend fun write(runtime: Runtime, value: Any?): Value {
		scope.set(name, when (value) {
			is RValue -> value.read(runtime)
			is Value -> value
			else -> ConstValue(value)
		})
		return NullValue()
	}

	override fun toString(): String = "ScopeValue(${name})"
}

class ConstValue(val constValue: Any?) : RValue {
	override suspend fun read(runtime: Runtime): Any? = constValue
	override fun toString(): String = "ConstValue(${constValue})"
}

class NullValue : Value {
	override fun toString(): String = "NullValue()"
}

class ListSetter(val array: ListValue, val key: Int) : RValue, LValue {
	override suspend fun read(runtime: Runtime): Any? = array.listValue[key]
	override suspend fun write(runtime: Runtime, value: Any?): Value {
		if (value is Value)
			array.listValue[key] = value
		else
			array.listValue[key] = ConstValue(value)

		return NullValue()
	}
}

class ListValue(val listValue: MutableList<Value> = mutableListOf()) : Value {
	override fun toString(): String = "List(${listValue.fold("", {a,b -> "$a,$b"})})"
}

class DictSetter(val dict: DictValue, val key: Any) : RValue, LValue {
	override suspend fun read(runtime: Runtime): Any? {
		val readValue = dict.map[key]
		return if (readValue is FuncValue) {
				val funcScope = ConstScope(readValue.capturedScope)
				funcScope.setConstant("self", dict)
				return FuncValue(readValue.funcNode, funcScope)
			} else
				readValue
	}

	override suspend fun write(runtime: Runtime, value: Any?): Value {
		dict.map[key] =
			if (value is Value)
				value
			else
				ConstValue(value)

		return NullValue()
	}
}

class DictValue(val map: HashMap<Any, Value>, val readOnly: Boolean) : Value {
	override fun toString(): String = "DictValue(${map.entries.fold("", {a, b -> "$a,${b.key}:${b.value}"})})"
}

interface FlowControlValue : Value

class ReturnValue(val returnValue: Value) : FlowControlValue

class BreakValue : FlowControlValue

class ContinueValue : FlowControlValue

class ThrownValue(val thrownValue: Any?) : FlowControlValue, RValue {
	override suspend fun read(runtime: Runtime): Any? = thrownValue
}
