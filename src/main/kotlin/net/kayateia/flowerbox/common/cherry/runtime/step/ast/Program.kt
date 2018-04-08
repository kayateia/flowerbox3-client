/*
	Flowerbox
	Copyright 2018 Kayateia

	This code is licensed under the MIT license; please see LICENSE.md in the root of the project.
 */

package net.kayateia.flowerbox.common.cherry.runtime.step.ast

import net.kayateia.flowerbox.common.cherry.parser.AstNode
import net.kayateia.flowerbox.common.cherry.parser.AstProgram
import net.kayateia.flowerbox.common.cherry.runtime.Runtime
import net.kayateia.flowerbox.common.cherry.runtime.step.Step

object Program : Step {
	override suspend fun execute(runtime: Runtime, node: AstNode) = when (node) {
		is AstProgram -> {
			var last: Any? = null
			node.stmts.forEach {
				last = Step.toStep(it).execute(runtime, it)
				runtime.stepAdd()
			}
			last
		}
		else -> throw Exception("invalid: wrong AST type was passed to step (${node.javaClass.canonicalName}")
	}
}
