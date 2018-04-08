/*
	Flowerbox
	Copyright 2018 Kayateia

	This code is licensed under the MIT license; please see LICENSE.md in the root of the project.
 */

package net.kayateia.flowerbox.common.cherry.runtime

import java.util.*

class OpStack {
	private val stack = Stack<Any?>()

	fun push(value: Any?) {
		stack.push(value)
	}

	fun isEmpty() = stack.isEmpty()

	fun pop(): Any? = stack.pop()
}
