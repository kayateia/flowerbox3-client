/*
	Flowerbox
	Copyright 2018 Kayateia

	This code is licensed under the MIT license; please see LICENSE.md in the root of the project.
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
					opExec(node.op, left, right)
			}
		}
		else -> throw Exception("invalid: wrong AST type was passed to step (${node.javaClass.canonicalName}")
	}

	private fun opExecNum(left: Value, right: Value, op: (left: Double, right: Double) -> Double) =
		ConstValue(op(Coercion.toNum(Value.prim(left)), Coercion.toNum(Value.prim(right))))

	private fun opExecNumBool(left: Value, right: Value, op: (left: Double, right: Double) -> Boolean) =
		ConstValue(op(Coercion.toNum(Value.prim(left)), Coercion.toNum(Value.prim(right))))

	private fun opExec(op: String, left: Value, right: Value): Value = when (op) {
		"*" -> opExecNum(left, right) { l, r -> l * r }
		"/" -> opExecNum(left, right) { l, r -> l / r }
		"%" -> opExecNum(left, right) { l, r -> l % r }
		"+" -> {
			val leftPrim = Value.prim(left)
			val rightPrim = Value.prim(right)
			if (leftPrim is String || rightPrim is String) {
				ConstValue(Coercion.toString(leftPrim) + Coercion.toString(rightPrim))
			} else {
				opExecNum(left, right) { l, r -> l + r }
			}
		}
		"-" -> opExecNum(left, right) { l, r -> l - r }
		"<" -> opExecNumBool(left, right) { l, r -> l < r }
		"<=" -> opExecNumBool(left, right) { l, r -> l <= r }
		">" -> opExecNumBool(left, right) { l, r -> l > r }
		">=" -> opExecNumBool(left, right) { l, r -> l >= r }

		"==" -> ConstValue(Value.compare(left, right))
		"=" -> {
			when (left) {
				is LValue -> {
					val simplified = Value.root(right)
					left.write(simplified)
					simplified
				}
				else -> throw Exception("can't assign to RValue")
			}
		}
		else -> throw Exception("unsupported binary op ${op}")
	}
}
