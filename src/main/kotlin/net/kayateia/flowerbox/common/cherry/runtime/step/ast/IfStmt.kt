/*
	Flowerbox
	Copyright 2018 Kayateia

	This code is licensed under the MIT license; please see LICENSE.md in the root of the project.
 */

package net.kayateia.flowerbox.common.cherry.runtime.step.ast

import net.kayateia.flowerbox.common.cherry.parser.AstIfStmt
import net.kayateia.flowerbox.common.cherry.parser.AstNode
import net.kayateia.flowerbox.common.cherry.runtime.Coercion
import net.kayateia.flowerbox.common.cherry.runtime.FlowControlValue
import net.kayateia.flowerbox.common.cherry.runtime.Runtime
import net.kayateia.flowerbox.common.cherry.runtime.Value
import net.kayateia.flowerbox.common.cherry.runtime.step.Step

object IfStmt : Step {
	override suspend fun execute(runtime: Runtime, node: AstNode): Value = when (node) {
		is AstIfStmt -> {
			val result = Step.execList(runtime, node.exprs)
			if (result is FlowControlValue)
				result
			else {
				if (Coercion.toBool(Value.prim(result)))
					Step.exec(runtime, node.ifTrue)
				else
					Step.exec(runtime, node.ifElse)
			}
		}
		else -> throw Exception("invalid: wrong AST type was passed to step (${node.javaClass.canonicalName}")
	}
}
