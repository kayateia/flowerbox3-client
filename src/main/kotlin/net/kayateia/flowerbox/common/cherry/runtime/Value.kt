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

class IntrinsicValue(val delegate: (args: ArrayValue) -> Value) : Value {
	override var value: Any?
		get() = throw Exception("can't use intrinsic as RValue")
		set(value) {
			throw Exception("can't assign into an intrinsic")
		}
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

class ArrayValue(val arrayValue: List<RValue>) : Value {
	override var value: Any?
		get() = throw Exception("can't use array value as RValue")
		set(value) {
			throw Exception("can't assign into an array value")
		}

	override fun toString(): String = "ArrayValue(${arrayValue.fold("", {a,b -> "$a,$b"})})"
}

interface FlowControlValue : Value

class ReturnValue(val returnValue: Value) : FlowControlValue {
	override var value: Any?
		get() = returnValue
		set(value) {
			throw Exception("can't assign into a return value")
		}
}
