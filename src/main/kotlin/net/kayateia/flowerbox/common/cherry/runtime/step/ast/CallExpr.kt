package net.kayateia.flowerbox.common.cherry.runtime.step.ast

import net.kayateia.flowerbox.common.cherry.parser.AstCallExpr
import net.kayateia.flowerbox.common.cherry.parser.AstNode
import net.kayateia.flowerbox.common.cherry.runtime.*
import net.kayateia.flowerbox.common.cherry.runtime.scope.MapScope
import net.kayateia.flowerbox.common.cherry.runtime.step.Step

object CallExpr : Step {
	override suspend fun execute(runtime: Runtime, node: AstNode): Value = when (node) {
		is AstCallExpr -> {
			val func = Step.exec(runtime, node.left)?.value
			if (func !is FuncValue)
				throw Exception("can't execute a non-function (${func})")

			val argVals = node.args.map {
				Step.exec(runtime, it).rvalue
			}

			val paramScope = MapScope(func.capturedScope)
			paramScope.set("arguments", ArrayValue(argVals))

			// TODO: This is not quite right, because it will leave unused parameters unset.
			val zipped = func.funcNode.params.zip(node.args)
			zipped.forEach {
				val argValue = Step.exec(runtime, it.second)
				paramScope.set(it.first, argValue.value)
			}

			runtime.scopePush(paramScope)

			val rv = Step.exec(runtime, func.funcNode.body)

			runtime.scopePop(paramScope)

			when (rv) {
				is ReturnValue -> rv.returnValue.rvalue
				else -> rv.rvalue
			}
		}
		else -> throw Exception("invalid: wrong AST type was passed to step (${node.javaClass.canonicalName}")
	}
}
