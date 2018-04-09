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
			is AstProgram				-> Program
			is AstFuncDecl				-> FuncDecl
			is AstBlock					-> Block
			is AstVarStmt				-> VarStmt
			is AstEmptyStmt				-> NoOp
			is AstExprStmt				-> ExprStmt
			is AstIfStmt				-> IfStmt
			is AstForSeq				-> ForSeq
			is AstForVarSeq				-> ForSeq
			is AstReturnStmt			-> ReturnStmt

			is AstVarDecl				-> VarDecl

			is AstFuncExpr				-> FuncExpr
			//is AstIndexExpr				-> IndexExpr
			is AstCallExpr				-> CallExpr
			is AstPostExpr				-> PostExpr
			//is AstPreExpr				-> PreExpr
			is AstUnaryExpr				-> UnaryExpr
			is AstBinaryExpr			-> BinaryExpr
			is AstLazyBinaryExpr		-> LazyBinaryExpr
			is AstIdExpr				-> IdExpr

			is AstLiteralExpr			-> LiteralExpr
			is AstExprListExpr			-> ExprListExpr

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
				if (last is ReturnValue)
					break
			}
			return last ?: NullValue()
		}
	}
}
