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
	fun execute(runtime: Runtime)

	companion object {
		fun toStep(node: AstNode): Step = when(node) {
			is AstProgram				-> Program(node)
			is AstFuncDecl				-> FuncDecl(node)
			is AstBlock					-> Block(node)
			is AstVarStmt				-> VarStmt(node)
			is AstEmptyStmt				-> NoOp()
			is AstExprStmt				-> ExprStmt(node)
			is AstIfStmt				-> IfStmt(node)

			//is AstFuncExpr				-> FuncExpr(node)
			//is AstIndexExpr				-> IndexExpr(node)
			//is AstPostExpr				-> PostExpr(node)
			//is AstPreExpr				-> PreExpr(node)
			is AstUnaryExpr				-> UnaryExpr(node)
			is AstBinaryExpr			-> BinaryExpr(node)

			is AstLiteralExpr			-> LiteralExpr(node)
			is AstExprListExpr			-> ExprListExpr(node)

			else -> {
				throw Exception("invalid step type ${node.javaClass.canonicalName}")
			}
		}
	}
}
