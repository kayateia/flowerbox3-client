/*
	Flowerbox
	Copyright 2018 Kayateia

	This code is licensed under the MIT license; please see LICENSE.md in the root of the project.
 */

package net.kayateia.flowerbox.common.cherry.runtime.step.ast

import net.kayateia.flowerbox.common.cherry.parser.AstIndexExpr
import net.kayateia.flowerbox.common.cherry.parser.AstNode
import net.kayateia.flowerbox.common.cherry.runtime.*
import net.kayateia.flowerbox.common.cherry.runtime.step.Step

object IndexExpr : Step {
	override suspend fun execute(runtime: Runtime, node: AstNode): Value = when (node) {
		is AstIndexExpr -> {
			val left = Value.root(Step.exec(runtime, node.left))
			if (left is ThrownValue)
				left
			else {
				val index = Value.root(Step.execList(runtime, node.index))
				if (index is ThrownValue)
					index
				else {
					when (left) {
						is DictValue -> objectIndex(left, Value.prim(index))
						is ListValue -> arrayIndex(left, Value.prim(index))
						else -> throw Exception("can't index into non-object, non-array")
					}
				}
			}
		}
		else -> throw Exception("invalid: wrong AST type was passed to step (${node.javaClass.canonicalName})")
	}

	private fun objectIndex(left: DictValue, index: Any?): Value = when (index) {
		null -> throw Exception("can't index object with null")
		else -> DictSetter(left, index)
	}

	private fun arrayIndex(left: ListValue, index: Any?): Value = when (index) {
		is Double -> ListSetter(left, index.toInt())
		else -> throw Exception("can't index array with value $index")
	}
}
