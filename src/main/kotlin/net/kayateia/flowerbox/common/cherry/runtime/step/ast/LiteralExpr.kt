/*
	Flowerbox
	Copyright 2018 Kayateia

	This code is licensed under the MIT license; please see LICENSE.md in the root of the project.
 */

package net.kayateia.flowerbox.common.cherry.runtime.step.ast

import net.kayateia.flowerbox.common.cherry.parser.AstLiteralExpr
import net.kayateia.flowerbox.common.cherry.runtime.Runtime
import net.kayateia.flowerbox.common.cherry.runtime.step.Step

// This class is like, LITERALLY... literals.
class LiteralExpr(val node: AstLiteralExpr) : Step {
	override fun execute(runtime: Runtime) {
		runtime.opPush(node.value)
	}
}
