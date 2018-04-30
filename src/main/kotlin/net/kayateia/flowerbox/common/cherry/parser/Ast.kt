/*
	Flowerbox
	Copyright 2018 Kayateia

	This code is licensed under the MIT license; please see LICENSE.md in the root of the project.
 */

package net.kayateia.flowerbox.common.cherry.parser

import org.antlr.v4.runtime.ParserRuleContext

data class AstModule(val name: String)

// Line number and column information stored from each AST element.
class AstLoc(val module: AstModule?, val enclosingFunction: AstFuncExpr?, val line: Int, val col: Int) {
	override fun toString(): String {
		return "AstLoc(${module?.name ?: "<unnamed>"}, ${enclosingFunction?.id ?: "<inline>"}, line $line, col $col"
	}
	companion object {
		fun from(parser: Parser, parserRuleContext: ParserRuleContext): AstLoc = AstLoc(
			parser.module, parser.functionTop,
			parserRuleContext.start.line,
			parserRuleContext.start.charPositionInLine
		)
	}
}

// Root interface for all AST nodes
interface AstNode {
	val loc: AstLoc
}

// Overall program container
data class AstProgram(override val loc: AstLoc, val stmts: List<AstStatement>) : AstNode

// Statements
interface AstStatement : AstNode
data class AstFuncDecl(override val loc: AstLoc, val func: AstFuncExpr) : AstStatement
data class AstBlock(override val loc: AstLoc, val stmts: List<AstStatement>) : AstStatement
data class AstVarStmt(override val loc: AstLoc, val decls: List<AstVarDecl>) : AstStatement
data class AstEmptyStmt(override val loc: AstLoc) : AstStatement
data class AstExprStmt(override val loc: AstLoc, val exprs: List<AstExpr>) : AstStatement
data class AstIfStmt(override val loc: AstLoc, val exprs: List<AstExpr>,
				val ifTrue: AstStatement, val ifElse: AstStatement?) : AstStatement
data class AstDoWhileStmt(override val loc: AstLoc, val stmt: AstStatement, val exprs: List<AstExpr>) : AstStatement
data class AstWhileStmt(override val loc: AstLoc, val exprs: List<AstExpr>, val stmt: AstStatement) : AstStatement
data class AstForSeq(override val loc: AstLoc, val init: List<AstExpr>, val cond: List<AstExpr>,
				val next: List<AstExpr>, val stmt: AstStatement) : AstStatement
data class AstForVarSeq(override val loc: AstLoc, val decls: List<AstVarDecl>, val cond: List<AstExpr>,
						val next: List<AstExpr>, val stmt: AstStatement) : AstStatement
data class AstForIn(override val loc: AstLoc, val variable: AstExpr, val seq: List<AstExpr>, val stmt: AstStatement) : AstStatement
data class AstForVarIn(override val loc: AstLoc, val decl: AstVarDecl, val seq: List<AstExpr>, val stmt: AstStatement) : AstStatement
data class AstContinueStmt(override val loc: AstLoc) : AstStatement
data class AstBreakStmt(override val loc: AstLoc) : AstStatement
data class AstReturnStmt(override val loc: AstLoc, val exprs: List<AstExpr>) : AstStatement
data class AstWithStmt(override val loc: AstLoc, val exprs: List<AstExpr>, val stmt: AstStatement) : AstStatement
data class AstSwitchStmt(override val loc: AstLoc, val exprs: List<AstExpr>, val cases: List<AstSwitchCase>) : AstStatement
data class AstThrowStmt(override val loc: AstLoc, val exprs: List<AstExpr>) : AstStatement
data class AstTryStmt(override val loc: AstLoc, val block: AstBlock, val catch: AstCatch?, val finally: AstFinally?) : AstStatement

// Switch
data class AstSwitchCase(override val loc: AstLoc, val exprs: List<AstExpr>?, val stmts: List<AstStatement>) : AstStatement

// Try/catch/finally
data class AstCatch(override val loc: AstLoc, val id: String?, val block: AstBlock) : AstNode
data class AstFinally(override val loc: AstLoc, val block: AstBlock) : AstNode

// Variable declarations
data class AstVarDecl(override val loc: AstLoc, val id: String, val init: AstExpr?) : AstNode

// Expression Types
interface AstExpr : AstNode
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
// Note about AstFuncExpr: AstFuncExpr must exist before evaluating the contents of "body", so they
// can refer back to it. But we can't create AstFuncExpr without evaluating "body". So to resolve
// this difficulty, "body" is a var here so it can be replaced with the real contents later.
data class AstFuncExpr(override val loc: AstLoc, val id: String?, val params: List<String>?, var body: AstBlock) : AstExpr
data class AstIndexExpr(override val loc: AstLoc, val left: AstExpr, val index: List<AstExpr>) : AstExpr
data class AstDotExpr(override val loc: AstLoc, val left: AstExpr, val member: String) : AstExpr
data class AstCallExpr(override val loc: AstLoc, val left: AstExpr, val args: List<AstExpr>) : AstExpr
data class AstNewExpr(override val loc: AstLoc, val left: AstExpr, val args: List<AstExpr>) : AstExpr
data class AstPostExpr(override val loc: AstLoc, override val op: String, override val expr: AstExpr) : AstUnary
data class AstDeleteExpr(override val loc: AstLoc, val expr: AstExpr) : AstExpr
data class AstVoidExpr(override val loc: AstLoc, val expr: AstExpr) : AstExpr
data class AstTypeofExpr(override val loc: AstLoc, val expr: AstExpr) : AstExpr
data class AstPreExpr(override val loc: AstLoc, override val op: String, override val expr: AstExpr) : AstUnary
data class AstUnaryExpr(override val loc: AstLoc, override val op: String, override val expr: AstExpr) : AstUnary
data class AstBinaryExpr(override val loc: AstLoc, override val left: AstExpr, override val op: String, override val right: AstExpr) : AstBinary
data class AstLazyBinaryExpr(override val loc: AstLoc, override val left: AstExpr, override val op: String, override val right: AstExpr) : AstBinary
data class AstTernaryExpr(override val loc: AstLoc, val cond: AstExpr, val ifTrue: AstExpr, val ifFalse: AstExpr) : AstExpr
data class AstSelfExpr(override val loc: AstLoc) : AstExpr
data class AstIdExpr(override val loc: AstLoc, val id: String) : AstExpr
data class AstLiteralExpr(override val loc: AstLoc, val value: Any?) : AstExpr
data class AstListExpr(override val loc: AstLoc, val contents: List<AstExpr>) : AstExpr
data class AstDictExpr(override val loc: AstLoc, val values: List<AstDictAssignment>) : AstExpr
data class AstExprListExpr(override val loc: AstLoc, val exprs: List<AstExpr>) : AstExpr

// Dictionary definition
data class AstDictAssignment(override val loc: AstLoc, val name: Any, val value: AstExpr) : AstNode

// Namespaces
data class AstNamespace(override val loc: AstLoc, val fqcn: String) : AstStatement
data class AstUsing(override val loc: AstLoc, val fqcn: String) : AstStatement

// Classes
data class AstClassDecl(override val loc: AstLoc, val name: String, val base: String?, val body: List<AstClassBodyDecl>) : AstStatement

enum class AstScopeType {
	PUBLIC, PRIVATE, PROTECTED
}
enum class AstAccessorType {
	GET, SET
}
interface AstClassBodyDecl {
	val scope: AstScopeType
}

data class AstMethodDecl(override val loc: AstLoc, override val scope: AstScopeType, val static: Boolean, val body: AstFuncDecl) : AstNode, AstClassBodyDecl
data class AstFieldDecl(override val loc: AstLoc, override val scope: AstScopeType, val static: Boolean, val decls: List<AstVarDecl>) : AstNode, AstClassBodyDecl
data class AstAccessorDecl(override  val loc: AstLoc, override val scope: AstScopeType, val static: Boolean, val type: AstAccessorType, val name: String, val arg: String?, val body: List<AstStatement>) : AstNode, AstClassBodyDecl

