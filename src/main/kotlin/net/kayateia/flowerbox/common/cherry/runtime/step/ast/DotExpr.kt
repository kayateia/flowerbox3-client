/*
	Flowerbox
	Copyright 2018 Kayateia

	This code is licensed under the MIT license; please see LICENSE.md in the root of the project.
 */

package net.kayateia.flowerbox.common.cherry.runtime.step.ast

import net.kayateia.flowerbox.common.cherry.parser.AstDotExpr
import net.kayateia.flowerbox.common.cherry.parser.AstNode
import net.kayateia.flowerbox.common.cherry.runtime.*
import net.kayateia.flowerbox.common.cherry.runtime.step.Step

object DotExpr : Step {
	override suspend fun execute(runtime: Runtime, node: AstNode): Value = when (node) {
		is AstDotExpr -> {
			val left = Value.root(Step.exec(runtime, node.left))
			if (left is ThrownValue)
				left
			else {
				// TODO - insert special logic here for primitive methods. (num.toString, etc)
				when (left) {
					is DictValue -> DictSetter(left, node.member)
					else -> throw Exception("can't dot-notation a non-object ($left)")
				}
			}
		}
		else -> throw Exception("invalid: wrong AST type was passed to step (${node.javaClass.canonicalName})")
	}
}
