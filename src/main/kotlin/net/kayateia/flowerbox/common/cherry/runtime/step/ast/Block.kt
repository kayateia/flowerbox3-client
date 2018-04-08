/*
	Flowerbox
	Copyright 2018 Kayateia

	This code is licensed under the MIT license; please see LICENSE.md in the root of the project.
 */

package net.kayateia.flowerbox.common.cherry.runtime.step.ast

import net.kayateia.flowerbox.common.cherry.parser.AstBlock
import net.kayateia.flowerbox.common.cherry.runtime.Runtime
import net.kayateia.flowerbox.common.cherry.runtime.step.Step
import net.kayateia.flowerbox.common.cherry.runtime.step.helper.ScopePopper

class Block(val node: AstBlock) : Statement() {
	override fun execute(runtime: Runtime) {
		super.execute(runtime)
		runtime.scopePush()
		runtime.codePush(ScopePopper())
		node.stmts.reversed().forEach {
			runtime.codePush(Step.toStep(it))
		}
	}
}
