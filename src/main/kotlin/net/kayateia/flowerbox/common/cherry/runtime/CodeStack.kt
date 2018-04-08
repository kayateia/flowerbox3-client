/*
	Flowerbox
	Copyright 2018 Kayateia

	This code is licensed under the MIT license; please see LICENSE.md in the root of the project.
 */

package net.kayateia.flowerbox.common.cherry.runtime

import net.kayateia.flowerbox.common.cherry.runtime.step.Step
import java.util.*

class CodeStack {
	private val stack = Stack<Step>()

	fun push(step: Step) {
		stack.push(step)
	}

	fun isEmpty() = stack.isEmpty()

	fun pop(): Step = stack.pop()
}
