/*
	Flowerbox
	Copyright 2018 Kayateia

	This code is licensed under the MIT license; please see LICENSE.md in the root of the project.
 */

package net.kayateia.flowerbox.common.cherry.runtime

import net.kayateia.flowerbox.common.cherry.parser.AstNode
import java.util.*

data class StackFrame(val node: AstNode)

class LexicalStack {
	private val stack = Stack<AstNode>()

	fun push(node: AstNode) = stack.push(node)
	fun pop() = stack.pop()
	val top: AstNode get() = stack.peek()

	override fun toString(): String =
		stack.map { "  ${it}" }
			.fold("Lexical Stack:", { acc, s -> acc + "\n" + s })

	val stackTrace: String get() =
		stack.map { "  ${it.loc.module?.name ?: "<inline>"}:${it.loc.enclosingFunction?.id ?: "<inline>"} (line ${it.loc.line}, col ${it.loc.col})" }
			.fold("Stack Trace:", { acc, s -> acc + "\n" + s })
}

class LexicalStackToken(val runtime: Runtime, val node: AstNode) : AutoCloseable {
	override fun close() {
		runtime.lexicalPop(node)
	}
}
