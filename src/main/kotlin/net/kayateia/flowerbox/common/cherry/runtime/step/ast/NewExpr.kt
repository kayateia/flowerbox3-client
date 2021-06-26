/*
	Flowerbox
	Copyright 2018 Kayateia

	Please see LICENSE.txt in the root of the project for more details.
 */

package net.kayateia.flowerbox.common.cherry.runtime.step.ast

import net.kayateia.flowerbox.common.cherry.parser.AstNewExpr
import net.kayateia.flowerbox.common.cherry.parser.AstNode
import net.kayateia.flowerbox.common.cherry.runtime.*
import net.kayateia.flowerbox.common.cherry.runtime.step.Step

object NewExpr : Step {
	override suspend fun execute(runtime: Runtime, node: AstNode): Value = when (node) {
		is AstNewExpr -> doNew(runtime, node)
		else -> throw Exception("invalid: wrong AST type was passed to step (${node.javaClass.canonicalName})")
	}

	private suspend fun doNew(runtime: Runtime, node: AstNewExpr): Value {
		val classObj = runtime.library.lookup(node.fqcn, listOf(runtime.currentNamespace) + runtime.nsUsings)
		if (classObj == null || classObj !is ClassValue)
			throw Exception("can't instantiate non-class $classObj")

		val args = ListValue()
		for (a in node.args) {
			val argVal = Step.exec(runtime, a)
			if (argVal is ThrownValue)
				return argVal
			args.listValue += argVal
		}

		return classObj.construct(runtime, args)
	}
}
