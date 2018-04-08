package net.kayateia.flowerbox.common.cherry.runtime

import net.kayateia.flowerbox.common.cherry.runtime.scope.Scope

interface Value {
	var value: Any?
}

interface LValue : Value

class ScopeLValue(val scope: Scope, val name: String) : LValue {
	override var value: Any?
		get() = scope.get(name)
		set(value) {
			scope.set(name, value)
		}

	override fun toString(): String = "LValue(${name})"
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
