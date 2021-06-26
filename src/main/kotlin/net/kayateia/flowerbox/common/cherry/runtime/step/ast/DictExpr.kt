/*
	Flowerbox
	Copyright 2018 Kayateia

	Please see LICENSE.txt in the root of the project for more details.
 */

package net.kayateia.flowerbox.common.cherry.runtime.step.ast

import net.kayateia.flowerbox.common.cherry.parser.AstDictExpr
import net.kayateia.flowerbox.common.cherry.parser.AstNode
import net.kayateia.flowerbox.common.cherry.runtime.DictValue
import net.kayateia.flowerbox.common.cherry.runtime.Runtime
import net.kayateia.flowerbox.common.cherry.runtime.ThrownValue
import net.kayateia.flowerbox.common.cherry.runtime.Value
import net.kayateia.flowerbox.common.cherry.runtime.step.Step

object DictExpr : Step {
	override suspend fun execute(runtime: Runtime, node: AstNode): Value = when (node) {
		is AstDictExpr -> buildDict(runtime, node)
		else -> throw Exception("invalid: wrong AST type was passed to step (${node.javaClass.canonicalName})")
	}

	private suspend fun buildDict(runtime: Runtime, node: AstDictExpr): Value {
		val map = HashMap<Any, Value>()

		for (p in node.values) {
			val expr = Value.root(runtime, Step.exec(runtime, p.value))
			if (expr is ThrownValue)
				return expr

			map[p.name] = expr
		}

		return DictValue(map, readOnly = false)
	}
}
