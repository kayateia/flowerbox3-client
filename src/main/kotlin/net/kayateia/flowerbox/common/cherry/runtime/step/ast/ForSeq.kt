/*
	Flowerbox
	Copyright 2018 Kayateia

	Please see LICENSE.txt in the root of the project for more details.
 */

package net.kayateia.flowerbox.common.cherry.runtime.step.ast

import net.kayateia.flowerbox.common.cherry.parser.*
import net.kayateia.flowerbox.common.cherry.runtime.*
import net.kayateia.flowerbox.common.cherry.runtime.scope.MapScope
import net.kayateia.flowerbox.common.cherry.runtime.step.Step

object ForSeq : Step {
	override suspend fun execute(runtime: Runtime, node: AstNode): Value = when (node) {
		is AstForVarSeq -> {
			runtime.scopePush().use {
				Step.execList(runtime, node.decls)
				forCommon(runtime, node.cond, node.next, node.stmt)
			}
		}
		is AstForSeq -> {
			Step.execList(runtime, node.init)
			forCommon(runtime, node.cond, node.next, node.stmt)
		}
		else -> throw Exception("invalid: wrong AST type was passed to step (${node.javaClass.canonicalName}")
	}

	private suspend fun forCommon(runtime: Runtime, cond: List<AstExpr>, next: List<AstExpr>, stmt: AstStatement): Value {
		while (true) {
			val condValue = Coercion.toBool(Value.prim(runtime, Step.execList(runtime, cond)))
			if (!condValue)
				return NullValue()

			val bodyValue = Step.exec(runtime, stmt)
			if (bodyValue is FlowControlValue) {
				when (bodyValue) {
					is ReturnValue		-> return Value.root(runtime, bodyValue.returnValue)
					is ThrownValue		-> return bodyValue
					is ContinueValue	-> {}
					is BreakValue		-> return NullValue()
				}
			}

			Step.execList(runtime, next)
		}
	}
}
