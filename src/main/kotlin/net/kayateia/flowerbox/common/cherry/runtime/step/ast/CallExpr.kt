/*
	Flowerbox
	Copyright 2018 Kayateia

	This code is licensed under the MIT license; please see LICENSE.md in the root of the project.
 */

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
			runtime.lexicalPush(node).use {
				when (func) {
					is FuncValue -> executeCherry(runtime, node, func)
					is IntrinsicValue -> executeNative(runtime, node, func)
					else -> throw Exception("can't execute a non-function (${func})")
				}
			}
		}
		else -> throw Exception("invalid: wrong AST type was passed to step (${node.javaClass.canonicalName}")
	}

	private suspend fun getArgs(runtime: Runtime, node: AstCallExpr): ArrayValue {
		return ArrayValue(node.args.map {
			Step.exec(runtime, it).rvalue
		})
	}

	private suspend fun executeCherry(runtime: Runtime, node: AstCallExpr, func: FuncValue): Value {
		val args = getArgs(runtime, node)
		val paramScope = MapScope(func.capturedScope)
		paramScope.setLocal("arguments", args)

		val zipped = func.funcNode.params.zip(args.arrayValue)
		zipped.forEach {
			paramScope.setLocal(it.first, it.second)
		}
		func.funcNode.params.forEach {
			if (!paramScope.hasLocal(it))
				paramScope.setLocal(it, NullValue())
		}

		runtime.scopePush(paramScope).use {
			val rv = Step.exec(runtime, func.funcNode.body)
			return when (rv) {
				is ReturnValue -> rv.returnValue.rvalue
				else -> rv.rvalue
			}
		}
	}

	private suspend fun executeNative(runtime: Runtime, node: AstCallExpr, func: IntrinsicValue): Value {
		return func.delegate(getArgs(runtime, node))
	}
}
