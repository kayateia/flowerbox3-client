/*
	Flowerbox
	Copyright 2018 Kayateia

	This code is licensed under the MIT license; please see LICENSE.md in the root of the project.
 */

package net.kayateia.flowerbox.common.cherry.runtime.library

import net.kayateia.flowerbox.common.cherry.runtime.*

object Math {
	val members = listOf(
		NativeImpl("sys", "math", "sin", { r, _, _, _, p -> sin(r, p) }),
		NativeImpl("sys", "math", "cos", { r, _, _, _, p -> cos(r, p) })
	)

	val decls = """
		namespace sys;

		class math {
			public static native cos(d) {}
			public static native sin(d) {}
		}
	"""

	private suspend fun sin(runtime: Runtime, params: ListValue): Value {
		return ConstValue(java.lang.Math.sin(Coercion.toNum(Value.prim(runtime, params.listValue[0]))))
	}

	private suspend fun cos(runtime: Runtime, params: ListValue): Value {
		return ConstValue(java.lang.Math.cos(Coercion.toNum(Value.prim(runtime, params.listValue[0]))))
	}
}
