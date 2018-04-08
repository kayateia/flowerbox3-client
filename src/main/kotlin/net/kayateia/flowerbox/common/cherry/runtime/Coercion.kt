/*
	Flowerbox
	Copyright 2018 Kayateia

	This code is licensed under the MIT license; please see LICENSE.md in the root of the project.
 */

package net.kayateia.flowerbox.common.cherry.runtime

object Coercion {
	fun toBool(value: Any?): Boolean =
		!(value == null || value == 0.0 || value == false || value == "")

	fun toNum(value: Any?): Double = when (value) {
		null -> throw Exception("found null when looking for number")
		is Double -> value
		is Boolean -> if (value) 1.0 else 0.0
		is String -> throw Exception("found string when looking for number")
		else -> throw Exception("found type ${value.javaClass.canonicalName} instead of number")
	}

	fun toString(value: Any?): String? = when (value) {
		null -> null
		is String -> value
		else -> value.toString()
	}
}
