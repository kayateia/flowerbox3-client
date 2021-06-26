/*
	Flowerbox
	Copyright 2018 Kayateia

	Please see LICENSE.txt in the root of the project for more details.
 */

package net.kayateia.flowerbox.common.cherry.runtime.step.ast

import net.kayateia.flowerbox.common.cherry.parser.AstDotExpr
import net.kayateia.flowerbox.common.cherry.parser.AstNode
import net.kayateia.flowerbox.common.cherry.runtime.*
import net.kayateia.flowerbox.common.cherry.runtime.step.Step

object DotExpr : Step {
	override suspend fun execute(runtime: Runtime, node: AstNode): Value = when (node) {
		is AstDotExpr -> {
			val left = Value.root(runtime, Step.exec(runtime, node.left))
			if (left is ThrownValue)
				left
			else {
				// TODO - insert special logic here for primitive methods. (num.toString, etc)
				val gottenValue = when (left) {
					is DictValue -> DictSetter(left, node.member)
					is ClassValue -> ClassAccessor(left, node.member)
					is ObjectValue -> ObjectAccessor(left, node.member)
					is NamespaceValue -> left.map[node.member] ?: NullValue()
					else -> throw Exception("can't dot-notation a non-object ($left)")
				}
				when (gottenValue) {
					is FuncValue -> gottenValue.newSelf(left)
					else -> gottenValue
				}
			}
		}
		else -> throw Exception("invalid: wrong AST type was passed to step (${node.javaClass.canonicalName})")
	}
}
