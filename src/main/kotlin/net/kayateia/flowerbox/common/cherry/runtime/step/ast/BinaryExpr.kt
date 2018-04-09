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

	private fun opExec(op: String, left: Value, right: Value): Value = when (op) {
		"*" -> RValue(Coercion.toNum(left.value) * Coercion.toNum(right.value))
		"/" -> RValue(Coercion.toNum(left.value) / Coercion.toNum(right.value))
		"%" -> RValue(Coercion.toNum(left.value) % Coercion.toNum(right.value))
		"+" -> RValue(Coercion.toNum(left.value) + Coercion.toNum(right.value))	// TODO - strings
		"-" -> RValue(Coercion.toNum(left.value) - Coercion.toNum(right.value))
		"<" -> RValue(Coercion.toNum(left.value) < Coercion.toNum(right.value))
		"<=" -> RValue(Coercion.toNum(left.value) <= Coercion.toNum(right.value))
		">" -> RValue(Coercion.toNum(left.value) > Coercion.toNum(right.value))
		">=" -> RValue(Coercion.toNum(left.value) >= Coercion.toNum(right.value))
		"==" -> RValue(Coercion.toBool(left.value == right.value))					// TODO - non-Kotlin semantics
		"=" -> {
			when (left) {
				is LValue -> {
					left.value = right.value
					right.rvalue
				}
				else -> throw Exception("can't assign to RValue")
			}
		}
		else -> throw Exception("unsupported binary op ${op}")
	}
}
