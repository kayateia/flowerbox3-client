/*
	Flowerbox
	Copyright 2018 Kayateia

	This code is licensed under the MIT license; please see LICENSE.md in the root of the project.
 */

package net.kayateia.flowerbox.common.cherry.runtime.step.ast

import net.kayateia.flowerbox.common.cherry.parser.AstIfStmt
import net.kayateia.flowerbox.common.cherry.runtime.Coercion
import net.kayateia.flowerbox.common.cherry.runtime.Runtime
import net.kayateia.flowerbox.common.cherry.runtime.step.Step

class IfStmt(val node: AstIfStmt) : Statement() {
	override fun execute(runtime: Runtime) {
		super.execute(runtime)
		runtime.codePush(IfStmtDecider(node))
		node.exprs.reversed().forEach {
			runtime.codePush(Step.toStep(it))
		}
	}
}

class IfStmtDecider(val node: AstIfStmt) : Step {
	override fun execute(runtime: Runtime) {
		val result = runtime.opPop()
		if (Coercion.toBool(result))
			runtime.codePush(Step.toStep(node.ifTrue))
		else
			runtime.codePush(Step.toStep(node.ifElse))
	}
}
