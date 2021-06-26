/*
	Flowerbox
	Copyright 2018 Kayateia

	Please see LICENSE.txt in the root of the project for more details.
 */

package net.kayateia.flowerbox.common.cherry.runtime.step.ast

import net.kayateia.flowerbox.common.cherry.parser.AstIfStmt
import net.kayateia.flowerbox.common.cherry.parser.AstNode
import net.kayateia.flowerbox.common.cherry.runtime.*
import net.kayateia.flowerbox.common.cherry.runtime.step.Step

object IfStmt : Step {
	override suspend fun execute(runtime: Runtime, node: AstNode): Value = when (node) {
		is AstIfStmt -> {
			val result = Value.root(runtime, Step.execList(runtime, node.exprs))
			if (result is FlowControlValue)
				result
			else {
				if (Value.bool(runtime, result))
					Step.exec(runtime, node.ifTrue)
				else {
					if (node.ifElse != null)
						Step.exec(runtime, node.ifElse)
					else
						NullValue()
				}
			}
		}
		else -> throw Exception("invalid: wrong AST type was passed to step (${node.javaClass.canonicalName}")
	}
}
