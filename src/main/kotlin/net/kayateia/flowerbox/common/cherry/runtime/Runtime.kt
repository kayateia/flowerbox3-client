/*
	Flowerbox
	Copyright 2018 Kayateia

	This code is licensed under the MIT license; please see LICENSE.md in the root of the project.
 */

package net.kayateia.flowerbox.common.cherry.runtime

import net.kayateia.flowerbox.common.cherry.parser.AstProgram
import net.kayateia.flowerbox.common.cherry.runtime.scope.MapScope
import net.kayateia.flowerbox.common.cherry.runtime.scope.Scope
import net.kayateia.flowerbox.common.cherry.runtime.step.Step

class Runtime(val program: AstProgram) {
	val opStack = OpStack()
	val codeStack = CodeStack()
	val scopeStack = ScopeStack()

	private var started = false

	// Returns true if we've fully completed execution.
	fun execute(maxSteps: Int): Boolean {
		// Get us started, if nothing has been pushed.
		if (codeStack.isEmpty()) {
			if (started) {
				return true
			} else {
				codeStack.push(Step.toStep(program))
				started = true
			}
		}

		for (s in 0 until maxSteps) {
			if (codeStack.isEmpty())
				return true

			val nextStep = codeStack.pop()
			nextStep.execute(this)
		}

		return false
	}

	fun opPush(value: Any?) = opStack.push(value)
	fun opPop(): Any? = opStack.pop()

	fun scopePush(scope: Scope = MapScope(scopeStack.top)): Scope = scopeStack.push(scope)
	fun scopePop(): Scope = scopeStack.pop()

	fun codePush(step: Step) = codeStack.push(step)
	fun codePop(): Step = codeStack.pop()
}
