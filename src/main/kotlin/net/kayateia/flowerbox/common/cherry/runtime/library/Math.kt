/*
	Flowerbox
	Copyright 2018 Kayateia

	This code is licensed under the MIT license; please see LICENSE.md in the root of the project.
 */

package net.kayateia.flowerbox.common.cherry.runtime.library

import net.kayateia.flowerbox.common.cherry.runtime.*

object Math {
	val members = listOf(
		IntrinsicImpl(listOf("sys", "math"), "sin", { rt, i, p -> sin(rt, p) }),
		IntrinsicImpl(listOf("sys", "math"), "cos", { rt, i, p -> cos(rt, p) })
	)

	private fun sin(runtime: Runtime, params: ArrayValue): Value {
		return ConstValue(java.lang.Math.sin(Coercion.toNum(Value.prim(params.arrayValue[0]))))
	}

	private fun cos(runtime: Runtime, params: ArrayValue): Value {
		return ConstValue(java.lang.Math.cos(Coercion.toNum(Value.prim(params.arrayValue[0]))))
	}
}
