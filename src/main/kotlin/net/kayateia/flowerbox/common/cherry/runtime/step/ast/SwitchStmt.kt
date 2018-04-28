/*
	Flowerbox
	Copyright 2018 Kayateia

	This code is licensed under the MIT license; please see LICENSE.md in the root of the project.
 */

package net.kayateia.flowerbox.common.cherry.runtime.step.ast

import net.kayateia.flowerbox.common.cherry.parser.AstNode
import net.kayateia.flowerbox.common.cherry.parser.AstSwitchCase
import net.kayateia.flowerbox.common.cherry.parser.AstSwitchStmt
import net.kayateia.flowerbox.common.cherry.runtime.*
import net.kayateia.flowerbox.common.cherry.runtime.step.Step

object SwitchStmt : Step {
	override suspend fun execute(runtime: Runtime, node: AstNode): Value = when (node) {
		is AstSwitchStmt -> execSwitch(runtime, node)
		else -> throw Exception("invalid: wrong AST type was passed to step (${node.javaClass.canonicalName}")
	}

	private suspend fun execSwitch(runtime: Runtime, node: AstSwitchStmt): Value {
		val exprValue = Value.root(Step.execList(runtime, node.exprs))
		return if (exprValue is FlowControlValue)
				exprValue
			else
				execCases(runtime, exprValue, node.cases)
	}

	private suspend fun execCases(runtime: Runtime, exprValue: Value, cases: List<AstSwitchCase>): Value {
		// What we'll do here is just start looking for a matching case, and if
		// we find it or a default, start executing them until a break happens.
		val casesToExec = findCases(runtime, exprValue, cases)
		for (case in casesToExec) {
			// Execute the block.
			val blockValue = Value.root(Step.execList(runtime, case.stmts))

			// If we got a flow control statement, deal with it appropriately. Otherwise, keep on truckin'.
			when (blockValue) {
				is BreakValue -> return NullValue()
				is ContinueValue -> throw Exception("can't continue inside switch")
				is FlowControlValue -> return blockValue
			}
		}

		return NullValue()
	}

	private suspend fun findCases(runtime: Runtime, exprValue: Value, cases: List<AstSwitchCase>): List<AstSwitchCase> {
		var remaining = cases
		while (remaining.isNotEmpty()) {
			val head = remaining.first()
			if (head.exprs == null)
				return remaining

			val swValue = Value.root(Step.execList(runtime, head.exprs))
			if (Value.compare(exprValue, swValue))
				return remaining

			remaining = remaining.drop(1)
		}

		return listOf()
	}
}
