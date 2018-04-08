/*
	Flowerbox
	Copyright 2018 Kayateia

	This code is licensed under the MIT license; please see LICENSE.md in the root of the project.
 */

package net.kayateia.flowerbox.common.cherry.runtime.step.ast

import net.kayateia.flowerbox.common.cherry.parser.AstBinaryExpr
import net.kayateia.flowerbox.common.cherry.parser.AstNode
import net.kayateia.flowerbox.common.cherry.runtime.Coercion
import net.kayateia.flowerbox.common.cherry.runtime.Runtime
import net.kayateia.flowerbox.common.cherry.runtime.step.Step

object BinaryExpr : Step {
	override suspend fun execute(runtime: Runtime, node: AstNode): Any? = when (node) {
		is AstBinaryExpr -> {
			val left = Step.toStep(node.left).execute(runtime, node.left)
			runtime.stepAdd()
			val right = Step.toStep(node.right).execute(runtime, node.right)
			runtime.stepAdd()
			opExec(node.op, left, right)
		}
		else -> throw Exception("invalid: wrong AST type was passed to step (${node.javaClass.canonicalName}")
	}

	private fun opExec(op: String, left: Any?, right: Any?): Any? = when (op) {
		"*" -> Coercion.toNum(left) * Coercion.toNum(right)
		"/" -> Coercion.toNum(left) / Coercion.toNum(right)
		"%" -> Coercion.toNum(left) % Coercion.toNum(right)
		"+" -> Coercion.toNum(left) + Coercion.toNum(right) // TODO - strings
		"-" -> Coercion.toNum(left) - Coercion.toNum(right)
		else -> throw Exception("unsupported binary op ${op}")
	}
}
