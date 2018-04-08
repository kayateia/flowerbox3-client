/*
	Flowerbox
	Copyright 2018 Kayateia

	This code is licensed under the MIT license; please see LICENSE.md in the root of the project.
 */

package net.kayateia.flowerbox.common.cherry.runtime.step.ast

import net.kayateia.flowerbox.common.cherry.parser.AstNode
import net.kayateia.flowerbox.common.cherry.parser.AstUnaryExpr
import net.kayateia.flowerbox.common.cherry.runtime.Coercion
import net.kayateia.flowerbox.common.cherry.runtime.Runtime
import net.kayateia.flowerbox.common.cherry.runtime.step.Step

object UnaryExpr : Step {
	override suspend fun execute(runtime: Runtime, node: AstNode): Any? = when (node) {
		is AstUnaryExpr -> {
			val value = Step.toStep(node.expr).execute(runtime, node.expr)
			runtime.stepAdd()
			opExec(node.op, value)
		}
		else -> throw Exception("invalid: wrong AST type was passed to step (${node.javaClass.canonicalName}")
	}

	private fun opExec(op: String, value: Any?): Any? = when (op) {
		"+" -> value
		"-" -> if (value is Double)
				-value
			else
				throw Exception("type error on unary -")
		"~" -> if (value is Double)
				throw Exception("unary ~ not supported")
			else
				throw Exception("type error on unary ~")
		"!" -> !Coercion.toBool(value)
		else -> throw Exception("invalid unary operator ${op}")
	}
}
