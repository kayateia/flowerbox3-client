/*
	Flowerbox
	Copyright 2018 Kayateia

	This code is licensed under the MIT license; please see LICENSE.md in the root of the project.
 */

package net.kayateia.flowerbox.common.cherry.runtime.step.ast

import net.kayateia.flowerbox.common.cherry.parser.*
import net.kayateia.flowerbox.common.cherry.runtime.*
import net.kayateia.flowerbox.common.cherry.runtime.scope.MapScope
import net.kayateia.flowerbox.common.cherry.runtime.step.Step

object ForSeq : Step {
	override suspend fun execute(runtime: Runtime, node: AstNode): Value = when (node) {
		is AstForVarSeq -> {
			val paramScope = MapScope(runtime.scope)
			runtime.scopePush(paramScope)
			Step.execList(runtime, node.decls)
			val result = forCommon(runtime, node.cond, node.next, node.stmt)
			runtime.scopePop(paramScope)

			result
		}
		is AstForSeq -> {
			Step.execList(runtime, node.init)
			forCommon(runtime, node.cond, node.next, node.stmt)
		}
		else -> throw Exception("invalid: wrong AST type was passed to step (${node.javaClass.canonicalName}")
	}

	private suspend fun forCommon(runtime: Runtime, cond: List<AstExpr>, next: List<AstExpr>, stmt: AstStatement): Value {
		while (true) {
			val condValue = Coercion.toBool(Step.execList(runtime, cond).value)
			if (!condValue)
				return NullValue()

			val bodyValue = Step.exec(runtime, stmt)
			if (bodyValue is ReturnValue)
				return bodyValue

			Step.execList(runtime, next)
		}
	}
}
