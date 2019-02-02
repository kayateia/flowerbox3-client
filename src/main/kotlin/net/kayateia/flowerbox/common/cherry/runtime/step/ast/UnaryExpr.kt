/*
	Flowerbox
	Copyright 2018 Kayateia

	This code is licensed under the GNU Public License version 3 or higher.
	Please see LICENSE.txt in the root of the project for more details.
 */

package net.kayateia.flowerbox.common.cherry.runtime.step.ast

import net.kayateia.flowerbox.common.cherry.parser.AstNode
import net.kayateia.flowerbox.common.cherry.parser.AstUnaryExpr
import net.kayateia.flowerbox.common.cherry.runtime.*
import net.kayateia.flowerbox.common.cherry.runtime.step.Step

object UnaryExpr : Step {
	override suspend fun execute(runtime: Runtime, node: AstNode): Value = when (node) {
		is AstUnaryExpr -> {
			val value = Step.exec(runtime, node.expr)
			if (value is FlowControlValue)
				value
			else
				opExec(runtime, node.op, value)
		}
		else -> throw Exception("invalid: wrong AST type was passed to step (${node.javaClass.canonicalName}")
	}

	private suspend fun opExec(runtime: Runtime, op: String, value: Any?): Value = when (op) {
		"+" -> ConstValue(Coercion.toNum(Value.prim(runtime, value)))
		"-" -> if (value is Double)
				ConstValue(-value)
			else
				throw Exception("type error on unary -")
		"~" -> if (value is Double)
				throw Exception("unary ~ not supported")
			else
				throw Exception("type error on unary ~")
		"!" -> ConstValue(!Value.bool(runtime, value))
		else -> throw Exception("invalid unary operator ${op}")
	}
}
