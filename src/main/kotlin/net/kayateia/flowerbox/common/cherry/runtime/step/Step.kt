/*
	Flowerbox
	Copyright 2018 Kayateia

	This code is licensed under the MIT license; please see LICENSE.md in the root of the project.
 */

package net.kayateia.flowerbox.common.cherry.runtime.step

import net.kayateia.flowerbox.common.cherry.parser.*
import net.kayateia.flowerbox.common.cherry.runtime.*
import net.kayateia.flowerbox.common.cherry.runtime.step.ast.*
import net.kayateia.flowerbox.common.cherry.runtime.step.helper.NoOp

interface Step {
	suspend fun execute(runtime: Runtime, node: AstNode): Value

	companion object {
		fun toStep(node: AstNode): Step = when(node) {
			// Overall program container
			is AstProgram				-> Program

			// Statements
			is AstFuncDecl				-> FuncDecl
			is AstBlock					-> Block
			is AstVarStmt				-> VarStmt
			is AstEmptyStmt				-> NoOp
			is AstExprStmt				-> ExprStmt
			is AstIfStmt				-> IfStmt
			is AstDoWhileStmt			-> TODO()
			is AstWhileStmt				-> TODO()
			is AstForSeq				-> ForSeq
			is AstForVarSeq				-> ForSeq
			is AstForIn					-> TODO()
			is AstForVarIn				-> TODO()
			is AstContinueStmt			-> TODO()
			is AstBreakStmt				-> TODO()
			is AstReturnStmt			-> ReturnStmt
			is AstWithStmt				-> TODO()
			is AstSwitchStmt			-> TODO()
			is AstThrowStmt				-> TODO()
			is AstTryStmt				-> TODO()

			// Try/catch/finally
			is AstCatch					-> TODO()
			is AstFinally				-> TODO()

			// Variable declarations
			is AstVarDecl				-> VarDecl

			// Expressions
			is AstFuncExpr				-> FuncExpr
			is AstIndexExpr				-> TODO()
			is AstDotExpr				-> DotExpr
			is AstCallExpr				-> CallExpr
			is AstNewExpr				-> TODO()
			is AstPostExpr				-> PostExpr
			is AstDeleteExpr			-> TODO()
			is AstVoidExpr				-> TODO()
			is AstTypeofExpr			-> TODO()
			is AstPreExpr				-> TODO()
			is AstUnaryExpr				-> UnaryExpr
			is AstBinaryExpr			-> BinaryExpr
			is AstLazyBinaryExpr		-> LazyBinaryExpr
			is AstTernaryExpr			-> TODO()
			is AstThisExpr				-> TODO()
			is AstIdExpr				-> IdExpr
			is AstLiteralExpr			-> LiteralExpr
			is AstExprListExpr			-> ExprListExpr
			is AstArrayExpr				-> TODO()
			is AstObjectExpr			-> TODO()

			else -> {
				throw Exception("invalid step type ${node.javaClass.canonicalName}")
			}
		}

		suspend fun exec(runtime: Runtime, node: AstNode): Value {
			val value = toStep(node).execute(runtime, node)
			runtime.stepAdd()
			return value
		}

		suspend fun execList(runtime: Runtime, items: List<AstNode>): Value {
			var last: Value? = null
			for (item in items) {
				last = toStep(item).execute(runtime, item)
				runtime.stepAdd()
				if (last is FlowControlValue)
					break
			}
			return last ?: NullValue()
		}
	}
}
