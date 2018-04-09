/*
	Flowerbox
	Copyright 2018 Kayateia

	This code is licensed under the MIT license; please see LICENSE.md in the root of the project.
 */

package net.kayateia.flowerbox.common.cherry.runtime.step.ast

import net.kayateia.flowerbox.common.cherry.parser.AstNode
import net.kayateia.flowerbox.common.cherry.parser.AstPostExpr
import net.kayateia.flowerbox.common.cherry.runtime.*
import net.kayateia.flowerbox.common.cherry.runtime.step.Step

object PostExpr : Step {
	override suspend fun execute(runtime: Runtime, node: AstNode): Value = when (node) {
		is AstPostExpr -> {
			val left = Step.exec(runtime, node.expr)
			val orig = left.value

			val newVal = when (node.op) {
				"--" -> Coercion.toNum(orig) - 1
				"++" -> Coercion.toNum(orig) + 1
				else -> throw Exception("invalid post-operator ${node.op}")
			}
			left.value = RValue(newVal)

			RValue(orig)
		}
		else -> throw Exception("invalid: wrong AST type was passed to step (${node.javaClass.canonicalName}")
	}
}
