/*
	Flowerbox
	Copyright 2018 Kayateia

	Please see LICENSE.txt in the root of the project for more details.
 */

package net.kayateia.flowerbox.common.cherry.runtime.step.ast

import net.kayateia.flowerbox.common.cherry.parser.AstClassDecl
import net.kayateia.flowerbox.common.cherry.parser.AstNode
import net.kayateia.flowerbox.common.cherry.runtime.ClassValue
import net.kayateia.flowerbox.common.cherry.runtime.Runtime
import net.kayateia.flowerbox.common.cherry.runtime.Value
import net.kayateia.flowerbox.common.cherry.runtime.step.Step

object ClassDecl : Step {
	override suspend fun execute(runtime: Runtime, node: AstNode): Value = when (node) {
		is AstClassDecl -> {
			val classValue = ClassValue(node, runtime.currentNamespace, runtime.scope, listOf(runtime.currentNamespace) + runtime.nsUsings.toList(), false)
			runtime.library.rootPackages.addOne(runtime.currentNamespace.split('.'), node.name, classValue)
			classValue
		}
		else -> throw Exception("invalid: wrong AST type was passed to step (${node.javaClass.canonicalName})")
	}
}
