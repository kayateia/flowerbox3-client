/*
	Flowerbox
	Copyright 2018 Kayateia

	This code is licensed under the MIT license; please see LICENSE.md in the root of the project.
 */

package net.kayateia.flowerbox.common.cherry.runtime.step.ast

import net.kayateia.flowerbox.common.cherry.parser.AstFuncDecl
import net.kayateia.flowerbox.common.cherry.runtime.Runtime
import net.kayateia.flowerbox.common.cherry.runtime.step.Step

class FuncDecl(val node: AstFuncDecl) : Statement() {
	override fun execute(runtime: Runtime) {
		super.execute(runtime)
		runtime.codeStack.push(Step.toStep(node.func))
	}
}
