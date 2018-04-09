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
			val right = Step.exec(runtime, node.right)
			opExec(node.op, left, right)
		}
		else -> throw Exception("invalid: wrong AST type was passed to step (${node.javaClass.canonicalName}")
	}

	private fun opExecNum(left: Value, right: Value, op: (left: Double, right: Double) -> Double) =
		RValue(op(Coercion.toNum(Value.getRootValue(left)), Coercion.toNum(Value.getRootValue(right))))

	private fun opExecNumBool(left: Value, right: Value, op: (left: Double, right: Double) -> Boolean) =
		RValue(op(Coercion.toNum(Value.getRootValue(left)), Coercion.toNum(Value.getRootValue(right))))

	private fun opExec(op: String, left: Value, right: Value): Value = when (op) {
		"*" -> opExecNum(left, right) { l, r -> l * r }
		"/" -> opExecNum(left, right) { l, r -> l / r }
		"%" -> opExecNum(left, right) { l, r -> l % r }
		"+" -> opExecNum(left, right) { l, r -> l + r }		// TODO - strings
		"-" -> opExecNum(left, right) { l, r -> l - r }
		"<" -> opExecNumBool(left, right) { l, r -> l < r }
		"<=" -> opExecNumBool(left, right) { l, r -> l <= r }
		">" -> opExecNumBool(left, right) { l, r -> l > r }
		">=" -> opExecNumBool(left, right) { l, r -> l >= r }

		"==" -> RValue(Coercion.toBool(Value.getRootValue(left) == Value.getRootValue(right)))					// TODO - non-Kotlin semantics
		"=" -> {
			when (left) {
				is LValue -> {
					val simplified = RValue(Value.getRootValue(right))
					left.value = simplified
					simplified
				}
				else -> throw Exception("can't assign to RValue")
			}
		}
		else -> throw Exception("unsupported binary op ${op}")
	}
}
