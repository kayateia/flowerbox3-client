/*
	Flowerbox
	Copyright 2018 Kayateia

	This code is licensed under the MIT license; please see LICENSE.md in the root of the project.
 */

package net.kayateia.flowerbox.common.cherry.runtime.step.ast

import net.kayateia.flowerbox.common.cherry.parser.AstLazyBinaryExpr
import net.kayateia.flowerbox.common.cherry.parser.AstNode
import net.kayateia.flowerbox.common.cherry.runtime.*
import net.kayateia.flowerbox.common.cherry.runtime.step.Step

object LazyBinaryExpr : Step {
	override suspend fun execute(runtime: Runtime, node: AstNode): Value = when (node) {
		is AstLazyBinaryExpr -> {
			val left = Step.exec(runtime, node.left)
			if (left is FlowControlValue)
				left
			else {
				when (node.op) {
					// Only the || operator does the weird value passing thing.
					"||" -> {
						if (Coercion.toBool(left.value))
							left
						else {
							val right = Step.exec(runtime, node.right)
							if (right is FlowControlValue)
								right
							else
								right.rvalue
						}
					}
					"&&" -> {
						if (Coercion.toBool(left.value)) {
							val right = Step.exec(runtime, node.right)
							if (right is FlowControlValue)
								right
							else
								RValue(true)
						} else
							RValue(false)
					}
					else -> throw Exception("unknown lazy binop ${node.op}")
				}
			}
		}
		else -> throw Exception("invalid: wrong AST type was passed to step (${node.javaClass.canonicalName}")
	}
}
