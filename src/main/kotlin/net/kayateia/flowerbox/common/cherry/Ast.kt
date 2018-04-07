/*
	Flowerbox
	Copyright 2018 Kayateia

	This code is licensed under the MIT license; please see LICENSE.md in the root of the project.
 */

package net.kayateia.flowerbox.common.cherry

// Overall program container
data class AstProgram(val stmts: List<AstStatement>)

// Statements
interface AstStatement
data class AstFuncDecl(val func: AstFuncExpr) : AstStatement
data class AstBlock(val stmts: List<AstStatement>) : AstStatement
data class AstVarStmt(val decls: List<AstVarDecl>) : AstStatement
data class AstEmptyStmt(val mkh: Boolean = true) : AstStatement
data class AstExprStmt(val exprs: List<AstExpr>) : AstStatement
data class AstIfStmt(val exprs: List<AstExpr>,
					 val ifTrue: AstStatement, val ifElse: AstStatement) : AstStatement
data class AstDoWhileStmt(val stmt: AstStatement, val exprs: List<AstExpr>) : AstStatement
data class AstWhileStmt(val exprs: List<AstExpr>, val stmt: AstStatement) : AstStatement
data class AstForSeq(val init: List<AstExpr>, val cond: List<AstExpr>,
					 val next: List<AstExpr>, val stmt: AstStatement) : AstStatement
data class AstForVarSeq(val decls: List<AstVarDecl>, val cond: List<AstExpr>,
						val next: List<AstExpr>, val stmt: AstStatement) : AstStatement
data class AstForIn(val variable: AstExpr, val seq: List<AstExpr>, val stmt: AstStatement) : AstStatement
data class AstForVarIn(val decl: AstVarDecl, val seq: List<AstExpr>, val stmt: AstStatement) : AstStatement
data class AstContinueStmt(val mkh: Boolean = true) : AstStatement
data class AstBreakStmt(val mkh: Boolean = true) : AstStatement
data class AstReturnStmt(val exprs: List<AstExpr>) : AstStatement
data class AstWithStmt(val exprs: List<AstExpr>, val stmt: AstStatement) : AstStatement
data class AstSwitchStmt(val exprs: List<AstExpr>, val stmt: AstStatement) : AstStatement
data class AstThrowStmt(val exprs: List<AstExpr>) : AstStatement
data class AstTryStmt(val block: AstBlock, val catches: List<AstCatch>, val finally: AstFinally) : AstStatement

// Try/catch/finally
data class AstCatch(val id: String, val block: AstBlock)
data class AstFinally(val block: AstBlock)

// Variable declarations
data class AstVarDecl(val id: String, val init: AstExpr?)

// Expression Types
interface AstExpr
interface AstAry : AstExpr {
	val op: String
}
interface AstUnary : AstAry {
	val expr: AstExpr
}
interface AstBinary : AstAry {
	val left: AstExpr
	val right: AstExpr
}

// Concrete expressions
data class AstFuncExpr(val id: String?, val params: List<String>, val body: AstBlock) : AstExpr
data class AstIndexExpr(val left: AstExpr, val index: List<AstExpr>) : AstExpr
data class AstDotExpr(val left: AstExpr, val member: String) : AstExpr
data class AstArgExpr(val left: AstExpr, val args: List<AstExpr>) : AstExpr
data class AstNewExpr(val left: AstExpr, val args: List<AstExpr>) : AstExpr
data class AstPostExpr(override val op: String, override val expr: AstExpr) : AstUnary
data class AstDeleteExpr(val expr: AstExpr) : AstExpr
data class AstVoidExpr(val expr: AstExpr) : AstExpr
data class AstTypeofExpr(val expr: AstExpr) : AstExpr
data class AstPreExpr(override val op: String, override val expr: AstExpr) : AstUnary
data class AstUnaryExpr(override val op: String, override val expr: AstExpr) : AstUnary
data class AstBinaryExpr(override val left: AstExpr, override val op: String, override val right: AstExpr) : AstBinary
data class AstTernaryExpr(val cond: AstExpr, val ifTrue: AstExpr, val ifFalse: AstExpr) : AstExpr
data class AstThisExpr(val mkh: Boolean = true) : AstExpr
data class AstIdExpr(val id: String) : AstExpr
data class AstLiteralExpr(val value: Any?) : AstExpr
data class AstArrayExpr(val contents: List<AstExpr>) : AstExpr
data class AstObjectExpr(val value: Map<Any, AstObjectProperty>) : AstExpr
data class AstExprListExpr(val exprs: List<AstExpr>) : AstExpr

// Object definition
interface AstObjectProperty
data class AstObjectAssignment(val name: Any, val value: AstExpr) : AstObjectProperty
data class AstObjectGetter(val name: String, val block: AstBlock) : AstObjectProperty
data class AstObjectSetter(val name: String, val param: String, val block: AstBlock) : AstObjectProperty
