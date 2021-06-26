/*
	Flowerbox
	Copyright 2018 Kayateia

	Please see LICENSE.txt in the root of the project for more details.
 */

package net.kayateia.flowerbox.common.cherry.runtime.step.ast

import net.kayateia.flowerbox.common.cherry.parser.AstNode
import net.kayateia.flowerbox.common.cherry.parser.AstProgram
import net.kayateia.flowerbox.common.cherry.runtime.Runtime
import net.kayateia.flowerbox.common.cherry.runtime.step.Step

object Program : Step {
	override suspend fun execute(runtime: Runtime, node: AstNode) = when (node) {
		is AstProgram -> Step.execList(runtime, node.stmts)
		else -> throw Exception("invalid: wrong AST type was passed to step (${node.javaClass.canonicalName}")
	}
}
