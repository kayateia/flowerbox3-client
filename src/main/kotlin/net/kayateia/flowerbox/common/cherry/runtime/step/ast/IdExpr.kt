/*
	Flowerbox
	Copyright 2018 Kayateia

	Please see LICENSE.txt in the root of the project for more details.
 */

package net.kayateia.flowerbox.common.cherry.runtime.step.ast

import net.kayateia.flowerbox.common.cherry.parser.AstIdExpr
import net.kayateia.flowerbox.common.cherry.parser.AstNode
import net.kayateia.flowerbox.common.cherry.parser.AstSelfExpr
import net.kayateia.flowerbox.common.cherry.parser.AstBaseExpr
import net.kayateia.flowerbox.common.cherry.runtime.NullValue
import net.kayateia.flowerbox.common.cherry.runtime.Runtime
import net.kayateia.flowerbox.common.cherry.runtime.ScopeLValue
import net.kayateia.flowerbox.common.cherry.runtime.Value
import net.kayateia.flowerbox.common.cherry.runtime.step.Step

object IdExpr : Step {
	override suspend fun execute(runtime: Runtime, node: AstNode): Value = when (node) {
		is AstIdExpr -> {
			if (runtime.scope.has(node.id))
				ScopeLValue(runtime.scope, node.id)
			else
				runtime.library.lookup(node.id, listOf(runtime.currentNamespace) + runtime.nsUsings)
					?: throw Exception("undeclared variable ${node.id}")
		}
		is AstBaseExpr -> {
			if (runtime.scope.has("base"))
				Value.box(runtime.scope.get("base"))
			else
				NullValue()
		}
		is AstSelfExpr -> {
			if (runtime.scope.has("self"))
				Value.box(runtime.scope.get("self"))
			else
				NullValue()
		}
		else -> throw Exception("invalid: wrong AST type was passed to step (${node.javaClass.canonicalName}")
	}
}
