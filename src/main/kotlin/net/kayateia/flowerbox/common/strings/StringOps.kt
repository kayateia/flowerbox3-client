/*
	Flowerbox
	Copyright 2018 Kayateia

	Please see LICENSE.txt in the root of the project for more details.
 */

package net.kayateia.flowerbox.common.strings

fun String.slice(a: Int, b: Int): String {
	val start = if (a >= 0)
		a
	else
		length + a
	val end = if (b >= 0)
		b
	else
		length + b

	return substring(start, end)
}
