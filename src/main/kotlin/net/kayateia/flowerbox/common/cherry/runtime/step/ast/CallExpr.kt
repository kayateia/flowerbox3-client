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
			val func = Value.root(Step.exec(runtime, node.left))
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
			Value.root(Step.exec(runtime, it))
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
				is ReturnValue -> Value.root(rv.returnValue)
				is ThrownValue -> rv
				else -> Value.root(rv)
			}
		}
	}

	private suspend fun executeNative(runtime: Runtime, node: AstCallExpr, func: IntrinsicValue): Value {
		val implicits = MapScope()
		implicits.set("self", NullValue())
		return func.delegate(runtime, implicits, getArgs(runtime, node))
	}
}
