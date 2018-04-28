package net.kayateia.flowerbox.common.cherry.runtime.library

import net.kayateia.flowerbox.common.cherry.runtime.*

object Math {
	val members = listOf(
		IntrinsicImpl(listOf("sys", "math"), "sin", { rt, i, p -> sin(rt, p) }),
		IntrinsicImpl(listOf("sys", "math"), "cos", { rt, i, p -> cos(rt, p) })
	)

	private fun sin(runtime: Runtime, params: ArrayValue): Value {
		return RValue(java.lang.Math.sin(Coercion.toNum(params.arrayValue[0].rvalue)))
	}

	private fun cos(runtime: Runtime, params: ArrayValue): Value {
		return RValue(java.lang.Math.cos(Coercion.toNum(params.arrayValue[0].rvalue)))
	}
}
