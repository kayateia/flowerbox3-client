/*
	Flowerbox
	Copyright 2018 Kayateia

	This code is licensed under the MIT license; please see LICENSE.md in the root of the project.
 */

package net.kayateia.flowerbox.common.cherry.runtime.step.ast

import net.kayateia.flowerbox.common.cherry.parser.AstProgram
import net.kayateia.flowerbox.common.cherry.runtime.Runtime
import net.kayateia.flowerbox.common.cherry.runtime.step.Step

class Program(val node: AstProgram) : Step {
	override fun execute(runtime: Runtime) {
		node.stmts.forEach {
			runtime.codeStack.push(Step.toStep(it))
		}
	}
}
