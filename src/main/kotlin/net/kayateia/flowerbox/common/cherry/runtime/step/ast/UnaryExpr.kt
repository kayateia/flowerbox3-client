/*
	Flowerbox
	Copyright 2018 Kayateia

	This code is licensed under the MIT license; please see LICENSE.md in the root of the project.
 */

package net.kayateia.flowerbox.common.cherry.runtime.step.ast

import net.kayateia.flowerbox.common.cherry.parser.AstUnaryExpr
import net.kayateia.flowerbox.common.cherry.runtime.Coercion
import net.kayateia.flowerbox.common.cherry.runtime.Runtime
import net.kayateia.flowerbox.common.cherry.runtime.step.Step

class UnaryExpr(val node: AstUnaryExpr) : Step {
	override fun execute(runtime: Runtime) {
		runtime.codePush(UnaryOpExec(node))
		runtime.codePush(Step.toStep(node.expr))
	}
}

class UnaryOpExec(val node: AstUnaryExpr) : Step {
	override fun execute(runtime: Runtime) {
		val value = runtime.opPop()
		when (node.op) {
			"+" -> runtime.opPush(value)
			"-" -> if (value is Double) {
					runtime.opPush(-value)
				} else {
					throw Exception("type error on unary -")
				}
			"~" -> if (value is Double) {
					throw Exception("unary ~ not supported")
				} else {
					throw Exception("type error on unary ~")
				}
			"!" -> runtime.opPush(!Coercion.toBool(value))
			else -> {
				throw Exception("invalid unary operator ${node.op}")
			}
		}
	}
}
