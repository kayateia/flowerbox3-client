/*
	Flowerbox
	Copyright 2018 Kayateia

	This code is licensed under the MIT license; please see LICENSE.md in the root of the project.
 */

package net.kayateia.flowerbox.common.cherry.parser

import net.kayateia.flowerbox.common.cherry.antlr.CherryParser.*
import net.kayateia.flowerbox.common.strings.*

fun ProgramContext.toAst(p: Parser): AstProgram = AstProgram(AstLoc.from(p, this), sourceElements().toAst(p))

fun SourceElementsContext.toAst(p: Parser): List<AstStatement> = sourceElement().map { it.toAst(p) }
fun SourceElementContext.toAst(p: Parser): AstStatement {
	return when (this) {
		is SourceElementStatementContext -> statement().toAst(p)
		is SourceElementFunctionDeclContext -> functionDeclaration().toAst(p)
		else -> {
			println("${this.text}, ${this.javaClass.canonicalName}")
			throw Exception("invalid source element type")
		}
	}
}

fun StatementContext.toAst(p: Parser): AstStatement = when (this) {
	is BlockStmtContext		-> block().toAst(p)
	is VarStmtContext		-> AstVarStmt(AstLoc.from(p, this), variableStatement().variableDeclarationList().variableDeclaration().map { it.toAst(p) })
	is EmptyStmtContext		-> AstEmptyStmt(AstLoc.from(p, this))
	is ExprStmtContext		-> AstExprStmt(AstLoc.from(p, this), expressionStatement().expressionSequence().toAst(p))
	is IfStmtContext		-> ifStatement().toAst(p)
	is IterStmtContext		-> iterationStatement().toAst(p)
	is ContStmtContext		-> AstContinueStmt(AstLoc.from(p, this))
	is BreakStmtContext		-> AstBreakStmt(AstLoc.from(p, this))
	is RetStmtContext		-> AstReturnStmt(AstLoc.from(p, this), returnStatement().expressionSequence().toAst(p))
	is WithStmtContext		-> AstWithStmt(AstLoc.from(p, this), withStatement().expressionSequence().toAst(p), withStatement().statement().toAst(p))
	is LabelStmtContext		-> throw Exception("not supported")
	is SwitchStmtContext	-> AstSwitchStmt(AstLoc.from(p, this), switchStatement().expressionSequence().toAst(p), switchStatement().caseBlock().toAst(p))
	is ThrowStmtContext		-> AstThrowStmt(AstLoc.from(p, this), throwStatement().expressionSequence().toAst(p))
	is TryStmtContext		-> AstTryStmt(AstLoc.from(p, this), tryStatement().block().toAst(p), tryStatement().catchProduction()?.toAst(p), tryStatement().finallyProduction()?.toAst(p))
	is DebugStmtContext		-> throw Exception("not supported")
	else -> { println("${this.text}, ${this.javaClass.canonicalName}");  throw Exception("invalid statement element type") }
}

fun BlockContext.toAst(p: Parser): AstBlock = AstBlock(AstLoc.from(p, this), statementList().statement().map { it.toAst(p) })

fun CaseBlockContext.toAst(p: Parser): List<AstSwitchCase> {
	val results: MutableList<AstSwitchCase> = mutableListOf()
	caseClauses().forEach {
		it.caseClause().forEach {
			results += it.toAst(p)
		}
	}

	val defaultClause = defaultClause()?.toAst(p)
	if (defaultClause != null)
		results += defaultClause

	return results
}

fun CaseClauseContext.toAst(p: Parser): AstSwitchCase = AstSwitchCase(AstLoc.from(p, this), expressionSequence().toAst(p), statementList().statement().map { it.toAst(p) })
fun DefaultClauseContext.toAst(p: Parser): AstSwitchCase = AstSwitchCase(AstLoc.from(p, this), null, statementList().statement().map { it.toAst(p) })

fun CatchProductionContext.toAst(p: Parser): AstCatch = AstCatch(AstLoc.from(p, this), Identifier()?.text, block().toAst(p))
fun FinallyProductionContext.toAst(p: Parser): AstFinally = AstFinally(AstLoc.from(p, this), block().toAst(p))

fun IfStatementContext.toAst(p: Parser): AstIfStmt = AstIfStmt(AstLoc.from(p, this), expressionSequence().toAst(p), statement(0).toAst(p), statement(1)?.toAst(p))

fun IterationStatementContext.toAst(p: Parser) : AstStatement = when(this) {
	is DoStatementContext		-> AstDoWhileStmt(AstLoc.from(p, this), statement().toAst(p), expressionSequence().toAst(p))
	is WhileStatementContext	-> AstWhileStmt(AstLoc.from(p, this), expressionSequence().toAst(p), statement().toAst(p))
	is ForStatementContext		-> AstForSeq(AstLoc.from(p, this), expressionSequence()[0].toAst(p), expressionSequence()[1].toAst(p), expressionSequence()[2].toAst(p), statement().toAst(p))
	is ForVarStatementContext	-> AstForVarSeq(AstLoc.from(p, this), variableDeclarationList().toAst(p), expressionSequence()[0].toAst(p), expressionSequence()[1].toAst(p), statement().toAst(p))
	is ForInStatementContext	-> AstForIn(AstLoc.from(p, this), singleExpression().toAst(p), expressionSequence().toAst(p), statement().toAst(p))
	is ForVarInStatementContext	-> AstForVarIn(AstLoc.from(p, this), variableDeclaration().toAst(p), expressionSequence().toAst(p), statement().toAst(p))
	else -> { println("${this.text}, ${this.javaClass.canonicalName}");  throw Exception("invalid iteration element type") }
}

fun VariableDeclarationListContext.toAst(p: Parser): List<AstVarDecl> = variableDeclaration().map { it.toAst(p) }
fun VariableDeclarationContext.toAst(p: Parser): AstVarDecl = AstVarDecl(AstLoc.from(p, this), Identifier().text, initialiser()?.singleExpression()?.toAst(p))

fun ExpressionSequenceContext.toAst(p: Parser) : List<AstExpr> = this.singleExpression().map { it.toAst(p) }
fun SingleExpressionContext.toAst(p: Parser): AstExpr = when(this) {
	is FunctionExpressionContext			-> {
		val funcExpr = AstFuncExpr(AstLoc.from(p, this), Identifier()?.text, formalParameterList()?.toAst(p), AstBlock(AstLoc.from(p, this), listOf()))
		p.functionPush(funcExpr)
		funcExpr.body = AstBlock(funcExpr.body.loc, functionBody().sourceElements().toAst(p))
		p.functionPop(funcExpr)
		funcExpr
	}
	is MemberIndexExpressionContext			-> AstIndexExpr(AstLoc.from(p, this), singleExpression().toAst(p), expressionSequence().toAst(p))
	is MemberDotExpressionContext			-> AstDotExpr(AstLoc.from(p, this), singleExpression().toAst(p), identifierName().text)
	is ArgumentsExpressionContext			-> AstCallExpr(AstLoc.from(p, this), singleExpression().toAst(p), arguments().toAst(p))
	is NewExpressionContext					-> AstNewExpr(AstLoc.from(p, this), singleExpression().toAst(p), arguments().toAst(p))
	is PostIncrementExpressionContext		-> AstPostExpr(AstLoc.from(p, this), getChild(1).text, singleExpression().toAst(p))
	is PostDecreaseExpressionContext		-> AstPostExpr(AstLoc.from(p, this), getChild(1).text, singleExpression().toAst(p))
	is DeleteExpressionContext				-> AstDeleteExpr(AstLoc.from(p, this), singleExpression().toAst(p))
	is VoidExpressionContext				-> AstVoidExpr(AstLoc.from(p, this), singleExpression().toAst(p))
	is TypeofExpressionContext				-> AstTypeofExpr(AstLoc.from(p, this), singleExpression().toAst(p))
	is PreIncrementExpressionContext		-> AstPreExpr(AstLoc.from(p, this), getChild(0).text, singleExpression().toAst(p))
	is PreDecreaseExpressionContext			-> AstPreExpr(AstLoc.from(p, this), getChild(0).text, singleExpression().toAst(p))
	is UnaryPlusExpressionContext			-> AstUnaryExpr(AstLoc.from(p, this), getChild(0).text, singleExpression().toAst(p))
	is UnaryMinusExpressionContext			-> AstUnaryExpr(AstLoc.from(p, this), getChild(0).text, singleExpression().toAst(p))
	is BitNotExpressionContext				-> AstUnaryExpr(AstLoc.from(p, this), getChild(0).text, singleExpression().toAst(p))
	is NotExpressionContext					-> AstUnaryExpr(AstLoc.from(p, this), getChild(0).text, singleExpression().toAst(p))
	is MultiplicativeExpressionContext		-> AstBinaryExpr(AstLoc.from(p, this), singleExpression(0).toAst(p), getChild(1).text, singleExpression(1).toAst(p))
	is AdditiveExpressionContext			-> AstBinaryExpr(AstLoc.from(p, this), singleExpression(0).toAst(p), getChild(1).text, singleExpression(1).toAst(p))
	is BitShiftExpressionContext			-> AstBinaryExpr(AstLoc.from(p, this), singleExpression(0).toAst(p), getChild(1).text, singleExpression(1).toAst(p))
	is RelationalExpressionContext			-> AstBinaryExpr(AstLoc.from(p, this), singleExpression(0).toAst(p), getChild(1).text, singleExpression(1).toAst(p))
	is InstanceofExpressionContext			-> AstBinaryExpr(AstLoc.from(p, this), singleExpression(0).toAst(p), getChild(1).text, singleExpression(1).toAst(p))
	is InExpressionContext					-> AstBinaryExpr(AstLoc.from(p, this), singleExpression(0).toAst(p), getChild(1).text, singleExpression(1).toAst(p))
	is EqualityExpressionContext			-> AstBinaryExpr(AstLoc.from(p, this), singleExpression(0).toAst(p), getChild(1).text, singleExpression(1).toAst(p))
	is BitAndExpressionContext				-> AstBinaryExpr(AstLoc.from(p, this), singleExpression(0).toAst(p), getChild(1).text, singleExpression(1).toAst(p))
	is BitXOrExpressionContext				-> AstBinaryExpr(AstLoc.from(p, this), singleExpression(0).toAst(p), getChild(1).text, singleExpression(1).toAst(p))
	is BitOrExpressionContext				-> AstBinaryExpr(AstLoc.from(p, this), singleExpression(0).toAst(p), getChild(1).text, singleExpression(1).toAst(p))
	is LogicalAndExpressionContext			-> AstLazyBinaryExpr(AstLoc.from(p, this), singleExpression(0).toAst(p), getChild(1).text, singleExpression(1).toAst(p))
	is LogicalOrExpressionContext			-> AstLazyBinaryExpr(AstLoc.from(p, this), singleExpression(0).toAst(p), getChild(1).text, singleExpression(1).toAst(p))
	is TernaryExpressionContext				-> AstTernaryExpr(AstLoc.from(p, this), singleExpression(0).toAst(p), singleExpression(1).toAst(p), singleExpression(2).toAst(p))
	is AssignmentExpressionContext			-> AstBinaryExpr(AstLoc.from(p, this), singleExpression(0).toAst(p), getChild(1).text, singleExpression(1).toAst(p))
	is AssignmentOperatorExpressionContext	-> AstBinaryExpr(AstLoc.from(p, this), singleExpression(0).toAst(p), assignmentOperator().text, singleExpression(1).toAst(p))
	is ThisExpressionContext				-> AstThisExpr(AstLoc.from(p, this))
	is IdentifierExpressionContext			-> AstIdExpr(AstLoc.from(p, this), Identifier().text)
	is LiteralExpressionContext				-> AstLiteralExpr(AstLoc.from(p, this), literal().toAst(p))
	is ArrayLiteralExpressionContext		-> AstListExpr(AstLoc.from(p, this), arrayLiteral().toAst(p))
// TODO
// is ObjectLiteralExpressionContext -> AstDictExpr(objectLiteral().toAst(p))
	is ParenthesizedExpressionContext		-> AstExprListExpr(AstLoc.from(p, this), expressionSequence().toAst(p))
	else -> { println("${this.text}, ${this.javaClass.canonicalName}");  throw Exception("invalid expression element type") }
}

fun ArgumentsContext.toAst(p: Parser): List<AstExpr> = (argumentList()?.singleExpression()?.map { it.toAst(p) }) ?: listOf()
fun FormalParameterListContext.toAst(p: Parser): List<String> = Identifier().map { it.text }

fun LiteralContext.toAst(p: Parser): Any? = when {
	NullLiteral()?.text != null					-> null
	BooleanLiteral()?.text != null				-> BooleanLiteral().text!!.toBoolean()
	StringLiteral()?.text != null				-> StringLiteral().text.slice(1, -1)		// Includes the quotes
	RegularExpressionLiteral()?.text != null	-> RegularExpressionLiteral().text
	numericLiteral() != null					-> numericLiteral().toAst(p)
	else -> { println("${this.text}, ${this.javaClass.canonicalName}");  throw Exception("invalid literal element type") }
}
fun NumericLiteralContext.toAst(p: Parser): Double = when {
	DecimalLiteral() != null		-> DecimalLiteral().text.toDouble()
	HexIntegerLiteral() != null		-> HexIntegerLiteral().text.toDouble()
	OctalIntegerLiteral() != null	-> OctalIntegerLiteral().text.toDouble()
	else -> { println("${this.text}, ${this.javaClass.canonicalName}");  throw Exception("invalid numeric literal element type") }
}

fun ArrayLiteralContext.toAst(p: Parser) : List<AstExpr> = elementList().singleExpression().map { it.toAst(p) }

fun ObjectLiteralContext.toAst(p: Parser) : AstDictExpr {
	val map = HashMap<Any, AstDictProperty>()
	propertyNameAndValueList()?.let {
		it.propertyAssignment().map { it.toAst(p) }
	}
	return AstDictExpr(AstLoc.from(p, this), map)
}
fun PropertyAssignmentContext.toAst(p: Parser) : AstDictProperty = when (this) {
	is PropertyExpressionAssignmentContext	-> AstDictAssignment(AstLoc.from(p, this), propertyName().toAst(p), singleExpression().toAst(p))
// TODO
//is PropertyGetterContext -> AstObjectGetter(getter().foo, AstBlock(functionBody().sourceElements().toAst(p)))
//is PropertySetterContext -> AstObjectSetter(setter().foo, propertySetParameterList().text, AstBlock(functionBody().sourceElements().toAst(p)))
//	is PropertyGetterContext -> TODO()
//	is PropertySetterContext -> TODO()
	else -> { println("${this.text}, ${this.javaClass.canonicalName}");  throw Exception("invalid property assignment element type") }
}
fun PropertyNameContext.toAst(p: Parser) : Any = when {
	identifierName() != null		-> identifierName().text
	StringLiteral()?.text != null	-> StringLiteral().text
	numericLiteral() != null		-> numericLiteral().toAst(p)
	else -> { println("${this.text}, ${this.javaClass.canonicalName}");  throw Exception("invalid property name element type") }
}

fun FunctionDeclarationContext.toAst(p: Parser): AstFuncDecl {
	val funcExpr = AstFuncExpr(AstLoc.from(p, this), Identifier().text, (formalParameterList()?.toAst(p)) ?: listOf(), AstBlock(AstLoc.from(p, this), listOf()))
	p.functionPush(funcExpr)
	funcExpr.body = AstBlock(funcExpr.body.loc, functionBody().sourceElements().toAst(p))
	p.functionPop(funcExpr)
	return AstFuncDecl(AstLoc.from(p, this), funcExpr)
}
