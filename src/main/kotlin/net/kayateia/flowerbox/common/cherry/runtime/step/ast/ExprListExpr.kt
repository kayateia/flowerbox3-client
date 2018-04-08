/*
	Flowerbox
	Copyright 2018 Kayateia

	This code is licensed under the MIT license; please see LICENSE.md in the root of the project.
 */

package net.kayateia.flowerbox.common.cherry.runtime.step.ast

import net.kayateia.flowerbox.common.cherry.parser.AstExprListExpr
import net.kayateia.flowerbox.common.cherry.runtime.Runtime
import net.kayateia.flowerbox.common.cherry.runtime.step.Step

class ExprListExpr(val node: AstExprListExpr) : Step {
	override fun execute(runtime: Runtime) {
		node.exprs.reversed().forEach {
			runtime.codePush(Step.toStep(it))
		}
	}
}
