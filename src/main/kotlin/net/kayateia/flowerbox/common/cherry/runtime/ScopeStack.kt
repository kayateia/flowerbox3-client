/*
	Flowerbox
	Copyright 2018 Kayateia

	Please see LICENSE.txt in the root of the project for more details.
 */

package net.kayateia.flowerbox.common.cherry.runtime

import net.kayateia.flowerbox.common.cherry.runtime.scope.ConstScope
import net.kayateia.flowerbox.common.cherry.runtime.scope.MapScope
import net.kayateia.flowerbox.common.cherry.runtime.scope.Scope
import java.util.*

class ScopeStack {
	val constScope = ConstScope()
	val rootScope = MapScope(constScope)

	private val stack = Stack<Scope>()

	init {
		push(constScope)
		push(rootScope)
	}

	fun push(scope: Scope) = stack.push(scope)
	fun pop() = stack.pop()
	val top: Scope get() = stack.peek()

	override fun toString(): String =
		stack.map { "   ${it} (${it.javaClass.simpleName})" }
			.fold("Scope Stack:", { acc, s -> acc + "\n" + s })
}

class ScopeStackToken(val runtime: Runtime, val scope: Scope) : AutoCloseable {
	override fun close() {
		runtime.scopePop(scope)
	}
}
