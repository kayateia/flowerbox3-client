/*
	Flowerbox
	Copyright 2018 Kayateia

	This code is licensed under the MIT license; please see LICENSE.md in the root of the project.
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
		"Scope Stack:\n" +
		stack.map { "   ${it} (${it.javaClass.canonicalName})" }
			.fold("", { acc, s -> acc + "\n" + s })
}
