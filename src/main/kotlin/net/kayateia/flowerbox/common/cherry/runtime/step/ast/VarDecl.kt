package net.kayateia.flowerbox.common.cherry.runtime.step.ast

import net.kayateia.flowerbox.common.cherry.parser.AstNode
import net.kayateia.flowerbox.common.cherry.parser.AstVarDecl
import net.kayateia.flowerbox.common.cherry.runtime.NullValue
import net.kayateia.flowerbox.common.cherry.runtime.Runtime
import net.kayateia.flowerbox.common.cherry.runtime.ScopeLValue
import net.kayateia.flowerbox.common.cherry.runtime.Value
import net.kayateia.flowerbox.common.cherry.runtime.step.Step

object VarDecl : Step {
	override suspend fun execute(runtime: Runtime, node: AstNode): Value = when (node) {
		is AstVarDecl -> {
			val init: Value = if (node.init != null)
					Step.exec(runtime, node.init)
				else
					NullValue()
			runtime.scope.set(node.id, init.value)

			ScopeLValue(runtime.scope, node.id)
		}
		else -> throw Exception("invalid: wrong AST type was passed to step (${node.javaClass.canonicalName}")
	}
}
