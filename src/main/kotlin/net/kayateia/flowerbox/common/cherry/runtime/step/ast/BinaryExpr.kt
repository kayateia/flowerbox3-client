/*
	Flowerbox
	Copyright 2018 Kayateia

	This code is licensed under the GNU Public License version 3 or higher.
	Please see LICENSE.txt in the root of the project for more details.
 */

package net.kayateia.flowerbox.common.cherry.runtime.step.ast

import net.kayateia.flowerbox.common.cherry.parser.AstBinaryExpr
import net.kayateia.flowerbox.common.cherry.parser.AstNode
import net.kayateia.flowerbox.common.cherry.runtime.*
import net.kayateia.flowerbox.common.cherry.runtime.step.Step

object BinaryExpr : Step {
	override suspend fun execute(runtime: Runtime, node: AstNode): Value = when (node) {
		is AstBinaryExpr -> {
			val left = Step.exec(runtime, node.left)
			if (left is ThrownValue)
				left
			else {
				val right = Step.exec(runtime, node.right)
				if (right is ThrownValue)
					right
				else
					opExec(runtime, node.op, left, right)
			}
		}
		else -> throw Exception("invalid: wrong AST type was passed to step (${node.javaClass.canonicalName}")
	}

	private suspend fun opExecNum(runtime: Runtime, left: Value, right: Value, op: (left: Double, right: Double) -> Double) =
		ConstValue(op(Coercion.toNum(Value.prim(runtime, left)), Coercion.toNum(Value.prim(runtime, right))))

	private suspend fun opExecNumBool(runtime: Runtime, left: Value, right: Value, op: (left: Double, right: Double) -> Boolean) =
		ConstValue(op(Coercion.toNum(Value.prim(runtime, left)), Coercion.toNum(Value.prim(runtime, right))))

	private suspend fun opExec(runtime: Runtime, op: String, left: Value, right: Value): Value = when (op) {
		"*" -> opExecNum(runtime, left, right) { l, r -> l * r }
		"/" -> opExecNum(runtime, left, right) { l, r -> l / r }
		"%" -> opExecNum(runtime, left, right) { l, r -> l % r }
		"+" -> {
			val leftPrim = Value.prim(runtime, left)
			val rightPrim = Value.prim(runtime, right)
			if (leftPrim is String || rightPrim is String) {
				ConstValue(Coercion.toString(leftPrim) + Coercion.toString(rightPrim))
			} else {
				opExecNum(runtime, left, right) { l, r -> l + r }
			}
		}
		"-" -> opExecNum(runtime, left, right) { l, r -> l - r }
		"<" -> opExecNumBool(runtime, left, right) { l, r -> l < r }
		"<=" -> opExecNumBool(runtime, left, right) { l, r -> l <= r }
		">" -> opExecNumBool(runtime, left, right) { l, r -> l > r }
		">=" -> opExecNumBool(runtime, left, right) { l, r -> l >= r }

		"==" -> ConstValue(Value.compare(runtime, left, right))
		"=" -> {
			when (left) {
				is LValue -> {
					val simplified = Value.root(runtime, right)
					left.write(runtime, simplified)
					simplified
				}
				else -> throw Exception("can't assign to RValue")
			}
		}
		else -> throw Exception("unsupported binary op ${op}")
	}
}
