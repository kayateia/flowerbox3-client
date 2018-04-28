/*
	Flowerbox
	Copyright 2018 Kayateia

	This code is licensed under the MIT license; please see LICENSE.md in the root of the project.
 */

package net.kayateia.flowerbox.common.cherry.runtime.step.ast

import net.kayateia.flowerbox.common.cherry.parser.AstNode
import net.kayateia.flowerbox.common.cherry.parser.AstThrowStmt
import net.kayateia.flowerbox.common.cherry.runtime.Runtime
import net.kayateia.flowerbox.common.cherry.runtime.ThrownValue
import net.kayateia.flowerbox.common.cherry.runtime.Value
import net.kayateia.flowerbox.common.cherry.runtime.step.Step

object ThrowStmt : Step {
	override suspend fun execute(runtime: Runtime, node: AstNode): Value = when (node) {
		is AstThrowStmt -> ThrownValue(Value.root(Step.execList(runtime, node.exprs)))
		else -> throw Exception("invalid: wrong AST type was passed to step (${node.javaClass.canonicalName}")
	}
}