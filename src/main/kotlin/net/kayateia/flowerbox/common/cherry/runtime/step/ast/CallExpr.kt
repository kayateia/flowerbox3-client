/*
	Flowerbox
	Copyright 2018 Kayateia

	This code is licensed under the GNU Public License version 3 or higher.
	Please see LICENSE.txt in the root of the project for more details.
 */

package net.kayateia.flowerbox.common.cherry.runtime.step.ast

import net.kayateia.flowerbox.common.cherry.parser.AstCallExpr
import net.kayateia.flowerbox.common.cherry.parser.AstNode
import net.kayateia.flowerbox.common.cherry.runtime.*
import net.kayateia.flowerbox.common.cherry.runtime.scope.MapScope
import net.kayateia.flowerbox.common.cherry.runtime.scope.Scope
import net.kayateia.flowerbox.common.cherry.runtime.step.Step

object CallExpr : Step {
	override suspend fun execute(runtime: Runtime, node: AstNode): Value = when (node) {
		is AstCallExpr -> {
			val funcOrig = Step.exec(runtime, node.left)
			val func = Value.root(runtime, funcOrig)
			runtime.lexicalPush(node).use {
				when (func) {
					is CValue -> func.call(runtime, getArgs(runtime, node))
					else -> throw Exception("can't execute a non-function (${func})")
				}
			}
		}
		else -> throw Exception("invalid: wrong AST type was passed to step (${node.javaClass.canonicalName}")
	}

	private suspend fun getArgs(runtime: Runtime, node: AstCallExpr): ListValue {
		return ListValue(node.args.map {
			Value.root(runtime, Step.exec(runtime, it))
		}.toMutableList())
	}
}
