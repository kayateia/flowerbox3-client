package net.kayateia.flowerbox.common.cherry.runtime.step.ast

import net.kayateia.flowerbox.common.cherry.parser.AstLazyBinaryExpr
import net.kayateia.flowerbox.common.cherry.parser.AstNode
import net.kayateia.flowerbox.common.cherry.runtime.Coercion
import net.kayateia.flowerbox.common.cherry.runtime.RValue
import net.kayateia.flowerbox.common.cherry.runtime.Runtime
import net.kayateia.flowerbox.common.cherry.runtime.Value
import net.kayateia.flowerbox.common.cherry.runtime.step.Step

object LazyBinaryExpr : Step {
	override suspend fun execute(runtime: Runtime, node: AstNode): Value = when (node) {
		is AstLazyBinaryExpr -> {
			val left = Step.exec(runtime, node.left)
			RValue(when (node.op) {
				"||" -> {
					if (Coercion.toBool(left.value))
						true
					else
						Coercion.toBool(Step.exec(runtime, node.right).value)
				}
				"&&" -> {
					if (Coercion.toBool(left.value))
						Coercion.toBool(Step.exec(runtime, node.right).value)
					else
						false
				}
				else -> throw Exception("unknown lazy binop ${node.op}")
			})
		}
		else -> throw Exception("invalid: wrong AST type was passed to step (${node.javaClass.canonicalName}")
	}
}
