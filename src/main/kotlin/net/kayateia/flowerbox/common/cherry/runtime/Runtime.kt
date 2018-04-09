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
import kotlin.coroutines.experimental.*
import kotlin.coroutines.experimental.intrinsics.*

class Runtime(val program: AstProgram) {
	val scopeStack = ScopeStack()
	var totalSteps = 0
	var maxSteps = 0
	var nextStep: Continuation<Unit>? = null

	var completed = false
	var result: Any? = null
	var exception: Throwable? = null

	init {
		scopeStack.constScope.setConstant("testfunc", IntrinsicValue({ args: ArrayValue -> RValue(5) }))
	}

	override fun toString(): String =
		"runtime: totalSteps ${totalSteps}, maxSteps ${maxSteps}, completed ${completed}, result ${result}, exc ${exception}\n${scopeStack}"

	// Returns true if we've fully completed execution.
	fun execute(maxSteps: Int = -1): Boolean {
		this.maxSteps = maxSteps
		totalSteps = 0

		val startFunc = suspend {
			Step.toStep(program).execute(this, program)
		}

		nextStep = startFunc.createCoroutine(object : Continuation<Any?> {
			override val context: CoroutineContext
				get() = EmptyCoroutineContext

			override fun resume(value: Any?) {
				result = value
				completed = true
			}

			override fun resumeWithException(exc: Throwable) {
				exception = exc
				completed = true
			}
		})
		nextStep!!.resume(Unit)

		return completed
	}

	fun executeMore(): Boolean {
		if (nextStep == null)
			throw Exception("Executing was never begun, can't continue")
		if (completed)
			throw Exception("Already completed, can't continue")

		nextStep!!.resume(Unit)

		return completed
	}

	fun scopePush(scope: Scope = MapScope(scopeStack.top)): Scope = scopeStack.push(scope)
	fun scopePop(scope: Scope? = null): Scope =
		if (scope != null) {
			val popped = scopeStack.pop()
			if (popped !== scope)
				throw Exception("scope stack push/pop mismatch")
			else
				popped
		} else
			scopeStack.pop()
	val scope: Scope get() = scopeStack.top

	suspend fun stepAdd() {
		if (maxSteps < 0)
			return

		totalSteps++
		if (totalSteps >= maxSteps)
			return suspendCoroutine {
				nextStep = it
				COROUTINE_SUSPENDED
			}
	}
}
