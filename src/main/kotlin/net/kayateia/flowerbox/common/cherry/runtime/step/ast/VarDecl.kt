/*
	Flowerbox
	Copyright 2018 Kayateia

	Please see LICENSE.txt in the root of the project for more details.
 */

package net.kayateia.flowerbox.common.cherry.runtime.step.ast

import net.kayateia.flowerbox.common.cherry.parser.AstNode
import net.kayateia.flowerbox.common.cherry.parser.AstVarDecl
import net.kayateia.flowerbox.common.cherry.runtime.*
import net.kayateia.flowerbox.common.cherry.runtime.step.Step

object VarDecl : Step {
	override suspend fun execute(runtime: Runtime, node: AstNode): Value = when (node) {
		is AstVarDecl -> {
			val init: Value = if (node.init != null)
					Step.exec(runtime, node.init)
				else
					NullValue()
			if (init is FlowControlValue)
				init
			else {
				runtime.scope.set(node.id, Value.root(runtime, init))

				ScopeLValue(runtime.scope, node.id)
			}
		}
		else -> throw Exception("invalid: wrong AST type was passed to step (${node.javaClass.canonicalName}")
	}
}
