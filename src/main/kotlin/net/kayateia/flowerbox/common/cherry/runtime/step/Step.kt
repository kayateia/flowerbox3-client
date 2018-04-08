/*
	Flowerbox
	Copyright 2018 Kayateia

	This code is licensed under the MIT license; please see LICENSE.md in the root of the project.
 */

package net.kayateia.flowerbox.common.cherry.runtime.step

import net.kayateia.flowerbox.common.cherry.parser.*
import net.kayateia.flowerbox.common.cherry.runtime.Runtime
import net.kayateia.flowerbox.common.cherry.runtime.step.ast.*
import net.kayateia.flowerbox.common.cherry.runtime.step.helper.NoOp

interface Step {
	suspend fun execute(runtime: Runtime, node: AstNode): Any?

	companion object {
		fun toStep(node: AstNode): Step = when(node) {
			is AstProgram				-> Program
			is AstFuncDecl				-> FuncDecl
			is AstBlock					-> Block
			is AstVarStmt				-> VarStmt
			is AstEmptyStmt				-> NoOp
			is AstExprStmt				-> ExprStmt
			is AstIfStmt				-> IfStmt

			//is AstFuncExpr				-> FuncExpr
			//is AstIndexExpr				-> IndexExpr
			//is AstPostExpr				-> PostExpr
			//is AstPreExpr				-> PreExpr
			is AstUnaryExpr				-> UnaryExpr
			is AstBinaryExpr			-> BinaryExpr

			is AstLiteralExpr			-> LiteralExpr
			is AstExprListExpr			-> ExprListExpr

			else -> {
				throw Exception("invalid step type ${node.javaClass.canonicalName}")
			}
		}

		suspend fun execList(runtime: Runtime, items: List<AstNode>): Any? {
			var last: Any? = null
			items.forEach {
				last = toStep(it).execute(runtime, it)
				runtime.stepAdd()
			}
			return last
		}
	}
}
