/*
	Flowerbox
	Copyright 2018 Kayateia

	This code is licensed under the MIT license; please see LICENSE.md in the root of the project.
 */

package net.kayateia.flowerbox.common.cherry.runtime.step.ast

import net.kayateia.flowerbox.common.cherry.parser.AstExpr
import net.kayateia.flowerbox.common.cherry.parser.AstNode
import net.kayateia.flowerbox.common.cherry.parser.AstPostExpr
import net.kayateia.flowerbox.common.cherry.parser.AstPreExpr
import net.kayateia.flowerbox.common.cherry.runtime.*
import net.kayateia.flowerbox.common.cherry.runtime.step.Step

object PrePostExpr : Step {
	override suspend fun execute(runtime: Runtime, node: AstNode): Value = when (node) {
		is AstPostExpr -> doExpr(runtime, node.op, node.expr, preValue = true)
		is AstPreExpr -> doExpr(runtime, node.op, node.expr, preValue = false)
		else -> throw Exception("invalid: wrong AST type was passed to step (${node.javaClass.canonicalName}")
	}

	private suspend fun doExpr(runtime: Runtime, op: String, expr: AstExpr, preValue: Boolean): Value {
		val left = Step.exec(runtime, expr)
		return when (left) {
			is ThrownValue -> left
			is LValue -> {
				val orig = Value.prim(runtime, left)

				val newVal = when (op) {
					"--" -> Coercion.toNum(orig) - 1
					"++" -> Coercion.toNum(orig) + 1
					else -> throw Exception("invalid post-operator $op")
				}
				left.write(runtime, newVal)

				if (preValue)
					ConstValue(orig)
				else
					ConstValue(newVal)
			}
			else -> throw Exception("can't increment/decrement non-lvalue $left")
		}
	}
}
