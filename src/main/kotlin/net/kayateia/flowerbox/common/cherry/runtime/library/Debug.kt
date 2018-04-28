/*
	Flowerbox
	Copyright 2018 Kayateia

	This code is licensed under the MIT license; please see LICENSE.md in the root of the project.
 */

package net.kayateia.flowerbox.common.cherry.runtime.library

import net.kayateia.flowerbox.common.cherry.runtime.*

object Debug {
	val members = listOf(
		IntrinsicImpl(listOf("sys", "dbg"), "println", { rt, i, p -> println(rt, p) })
	)

	private fun println(runtime: Runtime, params: ArrayValue): Value {
		println(params.arrayValue.map { it.toString() }.foldIndexed("", { i, a, b -> if (i == 0) b else a + b }))
		return NullValue()
	}
}