/*
	Flowerbox
	Copyright 2018 Kayateia

	This code is licensed under the MIT license; please see LICENSE.md in the root of the project.
 */

package net.kayateia.flowerbox.common.cherry.runtime.step.ast

import net.kayateia.flowerbox.common.cherry.parser.AstCatch
import net.kayateia.flowerbox.common.cherry.parser.AstFinally
import net.kayateia.flowerbox.common.cherry.parser.AstNode
import net.kayateia.flowerbox.common.cherry.parser.AstTryStmt
import net.kayateia.flowerbox.common.cherry.runtime.Runtime
import net.kayateia.flowerbox.common.cherry.runtime.ThrownValue
import net.kayateia.flowerbox.common.cherry.runtime.Value
import net.kayateia.flowerbox.common.cherry.runtime.step.Step

object TryStmt : Step {
	override suspend fun execute(runtime: Runtime, node: AstNode): Value = when (node) {
		is AstTryStmt -> execTry(runtime, node)
		else -> throw Exception("invalid: wrong AST type was passed to step (${node.javaClass.canonicalName}")
	}

	private suspend fun execTry(runtime: Runtime, node: AstTryStmt): Value {
		val blockValue = Value.root(Step.execList(runtime, node.block.stmts))

		val returnValue = when (blockValue) {
			is ThrownValue -> if (node.catch != null) execCatch(runtime, node.catch, blockValue) else blockValue
			else -> blockValue
		}

		if (node.finally != null)
			execFinally(runtime, node.finally)

		return returnValue
	}

	private suspend fun execCatch(runtime: Runtime, node: AstCatch, thrownValue: ThrownValue): Value {
		runtime.scopePush().use {
			if (node.id != null)
				it.scope.set(node.id, thrownValue.thrownValue)
			return Value.root(Step.execList(runtime, node.block.stmts))
		}
	}

	private suspend fun execFinally(runtime: Runtime, node: AstFinally): Value {
		runtime.scopePush().use {
			return Value.root(Step.execList(runtime, node.block.stmts))
		}
	}
}
