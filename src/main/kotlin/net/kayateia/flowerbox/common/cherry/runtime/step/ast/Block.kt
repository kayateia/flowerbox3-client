/*
	Flowerbox
	Copyright 2018 Kayateia

	This code is licensed under the GNU Public License version 3 or higher.
	Please see LICENSE.txt in the root of the project for more details.
 */

package net.kayateia.flowerbox.common.cherry.runtime.step.ast

import net.kayateia.flowerbox.common.cherry.parser.AstBlock
import net.kayateia.flowerbox.common.cherry.parser.AstNode
import net.kayateia.flowerbox.common.cherry.runtime.Runtime
import net.kayateia.flowerbox.common.cherry.runtime.scope.MapScope
import net.kayateia.flowerbox.common.cherry.runtime.step.Step

object Block : Step {
	override suspend fun execute(runtime: Runtime, node: AstNode) = when (node) {
		is AstBlock -> {
			runtime.scopePush().use {
				Step.execList(runtime, node.stmts)
			}
		}
		else -> throw Exception("invalid: wrong AST type was passed to step (${node.javaClass.canonicalName}")
	}
}
