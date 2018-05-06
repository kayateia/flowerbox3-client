/*
	Flowerbox
	Copyright 2018 Kayateia

	This code is licensed under the MIT license; please see LICENSE.md in the root of the project.
 */

package net.kayateia.flowerbox.common.cherry.runtime.step.ast

import net.kayateia.flowerbox.common.cherry.parser.AstNamespace
import net.kayateia.flowerbox.common.cherry.parser.AstNode
import net.kayateia.flowerbox.common.cherry.runtime.NullValue
import net.kayateia.flowerbox.common.cherry.runtime.Runtime
import net.kayateia.flowerbox.common.cherry.runtime.Value
import net.kayateia.flowerbox.common.cherry.runtime.step.Step

object NamespaceStmt : Step {
	override suspend fun execute(runtime: Runtime, node: AstNode): Value = when (node) {
		is AstNamespace -> {
			runtime.currentNamespace = node.fqcn
			NullValue()
		}
		else -> throw Exception("invalid: wrong AST type was passed to step (${node.javaClass.canonicalName})")
	}
}