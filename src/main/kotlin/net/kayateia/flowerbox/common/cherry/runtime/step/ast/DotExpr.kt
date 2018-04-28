package net.kayateia.flowerbox.common.cherry.runtime.step.ast

import net.kayateia.flowerbox.common.cherry.parser.AstDotExpr
import net.kayateia.flowerbox.common.cherry.parser.AstNode
import net.kayateia.flowerbox.common.cherry.runtime.*
import net.kayateia.flowerbox.common.cherry.runtime.step.Step

object DotExpr : Step {
	override suspend fun execute(runtime: Runtime, node: AstNode): Value = when (node) {
		is AstDotExpr -> {
			val left = Step.exec(runtime, node.left)
			if (left is ThrownValue)
				left
			else {
				// TODO - insert special logic here for primitive methods. (num.toString, etc)
				val leftUnpacked = Value.root(left)
				when (leftUnpacked) {
					is ObjectValue -> leftUnpacked.read(node.member) ?: NullValue()
					else -> throw Exception("can't dot-notation a non-object ($leftUnpacked)")
				}
			}
		}
		else -> throw Exception("invalid: wrong AST type was passed to step (${node.javaClass.canonicalName})")
	}
}
