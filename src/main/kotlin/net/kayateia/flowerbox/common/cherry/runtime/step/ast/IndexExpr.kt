/*
	Flowerbox
	Copyright 2018 Kayateia

	Please see LICENSE.txt in the root of the project for more details.
 */

package net.kayateia.flowerbox.common.cherry.runtime.step.ast

import net.kayateia.flowerbox.common.cherry.parser.AstIndexExpr
import net.kayateia.flowerbox.common.cherry.parser.AstNode
import net.kayateia.flowerbox.common.cherry.runtime.*
import net.kayateia.flowerbox.common.cherry.runtime.step.Step

object IndexExpr : Step {
	override suspend fun execute(runtime: Runtime, node: AstNode): Value = when (node) {
		is AstIndexExpr -> {
			val left = Value.root(runtime, Step.exec(runtime, node.left))
			if (left is ThrownValue)
				left
			else {
				val index = Value.root(runtime, Step.execList(runtime, node.index))
				if (index is ThrownValue)
					index
				else {
					val gottenValue = when (left) {
						is DictValue -> dictIndex(left, Value.prim(runtime, index))
						is ClassValue -> classIndex(left, Value.prim(runtime, index))
						is ObjectValue -> objectIndex(left, Value.prim(runtime, index))
						is ListValue -> arrayIndex(left, Value.prim(runtime, index))
						else -> throw Exception("can't index into non-object, non-array")
					}
					when (gottenValue) {
						is FuncValue -> gottenValue.newSelf(left)
						else -> gottenValue
					}
				}
			}
		}
		else -> throw Exception("invalid: wrong AST type was passed to step (${node.javaClass.canonicalName})")
	}

	private fun dictIndex(left: DictValue, index: Any?): Value = when (index) {
		null -> throw Exception("can't index dictionary with null")
		else -> DictSetter(left, index)
	}

	private fun classIndex(left: ClassValue, index: Any?): Value = when (index) {
		is String -> ClassAccessor(left, index)
		else -> throw Exception("can't index class with value $index")
	}

	private fun objectIndex(left: ObjectValue, index: Any?): Value = when (index) {
		is String -> ObjectAccessor(left, index)
		else -> throw Exception("can't index object with value $index")
	}

	private fun arrayIndex(left: ListValue, index: Any?): Value = when (index) {
		is Double -> ListSetter(left, index.toInt())
		else -> throw Exception("can't index array with value $index")
	}
}
