/*
	Flowerbox
	Copyright 2018 Kayateia

	This code is licensed under the MIT license; please see LICENSE.md in the root of the project.
 */

package net.kayateia.flowerbox.common.cherry.runtime.step.ast

import net.kayateia.flowerbox.common.cherry.parser.*
import net.kayateia.flowerbox.common.cherry.runtime.*
import net.kayateia.flowerbox.common.cherry.runtime.step.Step

object WhileStmt : Step {
	override suspend fun execute(runtime: Runtime, node: AstNode): Value = when (node) {
		is AstWhileStmt -> whileExec(runtime, node.exprs, node.stmt, checkBefore = true)
		is AstDoWhileStmt -> whileExec(runtime, node.exprs, node.stmt, checkBefore = false)
		else -> throw Exception("invalid: wrong AST type was passed to step (${node.javaClass.canonicalName}")
	}

	private suspend fun whileExec(runtime: Runtime, exprs: List<AstExpr>, stmt: AstStatement, checkBefore: Boolean): Value {
		var needCheck = checkBefore
		while (true) {
			if (needCheck) {
				val condValue = Coercion.toBool(Value.prim(runtime, Step.execList(runtime, exprs)))
				if (!condValue)
					return NullValue()
			}
			needCheck = true

			val bodyValue = Step.exec(runtime, stmt)
			if (bodyValue is FlowControlValue) {
				when (bodyValue) {
					is ReturnValue		-> return Value.root(runtime, bodyValue.returnValue)
					is ThrownValue		-> return bodyValue
					is ContinueValue	-> {}
					is BreakValue		-> return NullValue()
				}
			}
		}
	}
}
