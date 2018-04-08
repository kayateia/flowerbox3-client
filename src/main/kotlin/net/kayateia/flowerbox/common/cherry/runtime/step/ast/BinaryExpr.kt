/*
	Flowerbox
	Copyright 2018 Kayateia

	This code is licensed under the MIT license; please see LICENSE.md in the root of the project.
 */

package net.kayateia.flowerbox.common.cherry.runtime.step.ast

import net.kayateia.flowerbox.common.cherry.parser.AstBinaryExpr
import net.kayateia.flowerbox.common.cherry.runtime.Coercion
import net.kayateia.flowerbox.common.cherry.runtime.Runtime
import net.kayateia.flowerbox.common.cherry.runtime.step.Step

class BinaryExpr(val node: AstBinaryExpr) : Step {
	override fun execute(runtime: Runtime) {
		runtime.codePush(BinaryOpExec(node))
		runtime.codePush(Step.toStep(node.right))
		runtime.codePush(Step.toStep(node.left))
	}
}

class BinaryOpExec(val node: AstBinaryExpr) : Step {
	override fun execute(runtime: Runtime) {
		val right = runtime.opPop()
		val left = runtime.opPop()
		when (node.op) {
			"*" -> runtime.opPush(Coercion.toNum(left) * Coercion.toNum(right))
			"/" -> runtime.opPush(Coercion.toNum(left) / Coercion.toNum(right))
			"%" -> runtime.opPush(Coercion.toNum(left) % Coercion.toNum(right))
			"+" -> runtime.opPush(Coercion.toNum(left) + Coercion.toNum(right)) // TODO - strings
			"-" -> runtime.opPush(Coercion.toNum(left) - Coercion.toNum(right))
			else -> throw Exception("unsupported binary op ${node.op}")
		}
	}
}

