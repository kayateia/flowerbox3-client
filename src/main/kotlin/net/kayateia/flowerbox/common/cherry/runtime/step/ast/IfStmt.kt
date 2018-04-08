/*
	Flowerbox
	Copyright 2018 Kayateia

	This code is licensed under the MIT license; please see LICENSE.md in the root of the project.
 */

package net.kayateia.flowerbox.common.cherry.runtime.step.ast

import net.kayateia.flowerbox.common.cherry.parser.AstIfStmt
import net.kayateia.flowerbox.common.cherry.parser.AstNode
import net.kayateia.flowerbox.common.cherry.runtime.Coercion
import net.kayateia.flowerbox.common.cherry.runtime.Runtime
import net.kayateia.flowerbox.common.cherry.runtime.step.Step

object IfStmt : Step {
	override suspend fun execute(runtime: Runtime, node: AstNode): Any? = when (node) {
		is AstIfStmt -> {
			val result = Step.execList(runtime, node.exprs)
			if (Coercion.toBool(result))
				Step.toStep(node.ifTrue).execute(runtime, node.ifTrue)
			else
				Step.toStep(node.ifElse).execute(runtime, node.ifElse)
		}
		else -> throw Exception("invalid: wrong AST type was passed to step (${node.javaClass.canonicalName}")
	}
}
