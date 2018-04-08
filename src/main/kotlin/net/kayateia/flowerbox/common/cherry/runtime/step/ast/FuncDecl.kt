/*
	Flowerbox
	Copyright 2018 Kayateia

	This code is licensed under the MIT license; please see LICENSE.md in the root of the project.
 */

package net.kayateia.flowerbox.common.cherry.runtime.step.ast

import net.kayateia.flowerbox.common.cherry.parser.AstFuncDecl
import net.kayateia.flowerbox.common.cherry.parser.AstNode
import net.kayateia.flowerbox.common.cherry.runtime.Runtime
import net.kayateia.flowerbox.common.cherry.runtime.step.Step

object FuncDecl : Step {
	override suspend fun execute(runtime: Runtime, node: AstNode): Any? = when (node) {
		is AstFuncDecl -> {
			val rv = Step.toStep(node.func).execute(runtime, node.func)
			runtime.stepAdd()
			rv
		}
		else -> throw Exception("invalid: wrong AST type was passed to step (${node.javaClass.canonicalName}")
	}
}
