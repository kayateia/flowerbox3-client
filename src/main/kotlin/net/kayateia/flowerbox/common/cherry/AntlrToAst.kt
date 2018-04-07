/*
	Flowerbox
	Copyright 2018 Kayateia

	This code is licensed under the MIT license; please see LICENSE.md in the root of the project.
 */

package net.kayateia.flowerbox.common.cherry

import net.kayateia.flowerbox.common.cherry.antlr.CherryParser.*

fun ProgramContext.toAst(): AstProgram = AstProgram(sourceElements().toAst())

fun SourceElementsContext.toAst(): List<AstStatement> = sourceElement().map { it.toAst() }
fun SourceElementContext.toAst(): AstStatement {
	return when (this) {
		is SourceElementStatementContext -> statement().toAst()
		is SourceElementFunctionDeclContext -> functionDeclaration().toAst()
		else -> {
			println("${this.text}, ${this.javaClass.canonicalName}");
			throw Exception("invalid source element type")
		}
	}
}

fun StatementContext.toAst(): AstStatement = when (this) {
	is BlockStmtContext		-> AstBlock(block().statementList().statement().map { it.toAst() })
	is VarStmtContext		-> AstVarStmt(variableStatement().variableDeclarationList().variableDeclaration().map { it.toAst() })
	is EmptyStmtContext		-> AstEmptyStmt()
	is ExprStmtContext		-> AstExprStmt(expressionStatement().expressionSequence().toAst())
	is IfStmtContext		-> ifStatement().toAst()
	is IterStmtContext		-> iterationStatement().toAst()
	is ContStmtContext		-> AstContinueStmt()
	is BreakStmtContext		-> AstBreakStmt()
	is RetStmtContext		-> AstReturnStmt(returnStatement().expressionSequence().toAst())
	is WithStmtContext		-> AstWithStmt(withStatement().expressionSequence().toAst(), withStatement().statement().toAst())
	is LabelStmtContext		-> throw Exception("not supported")
	is SwitchStmtContext	-> AstEmptyStmt() // TODO
	is ThrowStmtContext		-> AstThrowStmt(throwStatement().expressionSequence().toAst())
	is TryStmtContext		-> AstEmptyStmt() // TODO
	is DebugStmtContext		-> throw Exception("not supported")
	else -> { println("${this.text}, ${this.javaClass.canonicalName}");  throw Exception("invalid statement element type") }
}

fun IfStatementContext.toAst(): AstIfStmt = AstIfStmt(expressionSequence().toAst(), statement(0).toAst(), statement(1).toAst())

fun IterationStatementContext.toAst() : AstStatement = when(this) {
	is DoStatementContext		-> AstDoWhileStmt(statement().toAst(), expressionSequence().toAst())
	is WhileStatementContext	-> AstWhileStmt(expressionSequence().toAst(), statement().toAst())
	is ForStatementContext		-> AstForSeq(expressionSequence().get(0).toAst(), expressionSequence().get(1).toAst(), expressionSequence().get(2).toAst(), statement().toAst())
	is ForVarStatementContext	-> AstForVarSeq(variableDeclarationList().toAst(), expressionSequence().get(0).toAst(), expressionSequence().get(1).toAst(), statement().toAst())
	is ForInStatementContext	-> AstForIn(singleExpression().toAst(), expressionSequence().toAst(), statement().toAst())
	is ForVarInStatementContext	-> AstForVarIn(variableDeclaration().toAst(), expressionSequence().toAst(), statement().toAst())
	else -> { println("${this.text}, ${this.javaClass.canonicalName}");  throw Exception("invalid iteration element type") }
}

fun VariableDeclarationListContext.toAst(): List<AstVarDecl> = variableDeclaration().map { it.toAst() }
fun VariableDeclarationContext.toAst(): AstVarDecl = AstVarDecl(Identifier().text, initialiser()?.singleExpression()?.toAst())

fun ExpressionSequenceContext.toAst() : List<AstExpr> = this.singleExpression().map { it.toAst() }
fun SingleExpressionContext.toAst(): AstExpr = when(this) {
	is FunctionExpressionContext			-> AstFuncExpr(Identifier()?.text, formalParameterList().toAst(), AstBlock(functionBody().sourceElements().toAst()))
	is MemberIndexExpressionContext			-> AstIndexExpr(singleExpression().toAst(), expressionSequence().toAst())
	is MemberDotExpressionContext			-> AstDotExpr(singleExpression().toAst(), identifierName().text)
	is ArgumentsExpressionContext			-> AstArgExpr(singleExpression().toAst(), arguments().toAst())
	is NewExpressionContext					-> AstNewExpr(singleExpression().toAst(), arguments().toAst())
	is PostIncrementExpressionContext		-> AstPostExpr(getChild(1).text, singleExpression().toAst())
	is PostDecreaseExpressionContext		-> AstPostExpr(getChild(1).text, singleExpression().toAst())
	is DeleteExpressionContext				-> AstDeleteExpr(singleExpression().toAst())
	is VoidExpressionContext				-> AstVoidExpr(singleExpression().toAst())
	is TypeofExpressionContext				-> AstTypeofExpr(singleExpression().toAst())
	is PreIncrementExpressionContext		-> AstPreExpr(getChild(0).text, singleExpression().toAst())
	is PreDecreaseExpressionContext			-> AstPreExpr(getChild(0).text, singleExpression().toAst())
	is UnaryPlusExpressionContext			-> AstUnaryExpr(getChild(0).text, singleExpression().toAst())
	is UnaryMinusExpressionContext			-> AstUnaryExpr(getChild(0).text, singleExpression().toAst())
	is BitNotExpressionContext				-> AstUnaryExpr(getChild(0).text, singleExpression().toAst())
	is NotExpressionContext					-> AstUnaryExpr(getChild(0).text, singleExpression().toAst())
	is MultiplicativeExpressionContext		-> AstBinaryExpr(singleExpression(0).toAst(), getChild(1).text, singleExpression(1).toAst())
	is AdditiveExpressionContext			-> AstBinaryExpr(singleExpression(0).toAst(), getChild(1).text, singleExpression(1).toAst())
	is BitShiftExpressionContext			-> AstBinaryExpr(singleExpression(0).toAst(), getChild(1).text, singleExpression(1).toAst())
	is RelationalExpressionContext			-> AstBinaryExpr(singleExpression(0).toAst(), getChild(1).text, singleExpression(1).toAst())
	is InstanceofExpressionContext			-> AstBinaryExpr(singleExpression(0).toAst(), getChild(1).text, singleExpression(1).toAst())
	is InExpressionContext					-> AstBinaryExpr(singleExpression(0).toAst(), getChild(1).text, singleExpression(1).toAst())
	is EqualityExpressionContext			-> AstBinaryExpr(singleExpression(0).toAst(), getChild(1).text, singleExpression(1).toAst())
	is BitAndExpressionContext				-> AstBinaryExpr(singleExpression(0).toAst(), getChild(1).text, singleExpression(1).toAst())
	is BitXOrExpressionContext				-> AstBinaryExpr(singleExpression(0).toAst(), getChild(1).text, singleExpression(1).toAst())
	is BitOrExpressionContext				-> AstBinaryExpr(singleExpression(0).toAst(), getChild(1).text, singleExpression(1).toAst())
	is LogicalAndExpressionContext			-> AstBinaryExpr(singleExpression(0).toAst(), getChild(1).text, singleExpression(1).toAst())
	is LogicalOrExpressionContext			-> AstBinaryExpr(singleExpression(0).toAst(), getChild(1).text, singleExpression(1).toAst())
	is TernaryExpressionContext				-> AstTernaryExpr(singleExpression(0).toAst(), singleExpression(1).toAst(), singleExpression(2).toAst())
	is AssignmentExpressionContext			-> AstBinaryExpr(singleExpression(0).toAst(), getChild(1).text, singleExpression(1).toAst())
	is AssignmentOperatorExpressionContext	-> AstBinaryExpr(singleExpression(0).toAst(), assignmentOperator().text, singleExpression(1).toAst())
	is ThisExpressionContext				-> AstThisExpr()
	is IdentifierExpressionContext			-> AstIdExpr(Identifier().text)
	is LiteralExpressionContext				-> AstLiteralExpr(literal().toAst())
	is ArrayLiteralExpressionContext		-> AstArrayExpr(arrayLiteral().toAst())
// TODO
// is ObjectLiteralExpressionContext -> AstObjectExpr(objectLiteral().toAst())
	is ParenthesizedExpressionContext		-> AstExprListExpr(expressionSequence().toAst())
	else -> { println("${this.text}, ${this.javaClass.canonicalName}");  throw Exception("invalid expression element type") }
}

fun ArgumentsContext.toAst(): List<AstExpr> = (argumentList()?.singleExpression()?.map { it.toAst() }) ?: listOf()
fun FormalParameterListContext.toAst(): List<String> = Identifier().map { it.text }

fun LiteralContext.toAst(): Any? = when {
	NullLiteral()?.text != null					-> null
	BooleanLiteral()?.text != null				-> BooleanLiteral().text.toBoolean()
	StringLiteral()?.text != null				-> StringLiteral().text
	RegularExpressionLiteral()?.text != null	-> RegularExpressionLiteral().text
	numericLiteral() != null					-> numericLiteral().toAst()
	else -> { println("${this.text}, ${this.javaClass.canonicalName}");  throw Exception("invalid literal element type") }
}
fun NumericLiteralContext.toAst(): Double = when {
	DecimalLiteral() != null		-> DecimalLiteral().text.toDouble()
	HexIntegerLiteral() != null		-> HexIntegerLiteral().text.toDouble()
	OctalIntegerLiteral() != null	-> OctalIntegerLiteral().text.toDouble()
	else -> { println("${this.text}, ${this.javaClass.canonicalName}");  throw Exception("invalid numeric literal element type") }
}

fun ArrayLiteralContext.toAst() : List<AstExpr> = elementList().singleExpression().map { it.toAst() }

fun ObjectLiteralContext.toAst() : AstObjectExpr {
	val map = HashMap<Any, AstObjectProperty>()
	propertyNameAndValueList()?.let {
		it.propertyAssignment().map { it.toAst() }
	}
	return AstObjectExpr(map)
}
fun PropertyAssignmentContext.toAst() : AstObjectProperty = when (this) {
	is PropertyExpressionAssignmentContext	-> AstObjectAssignment(propertyName().toAst(), singleExpression().toAst())
// TODO
//is PropertyGetterContext -> AstObjectGetter(getter().foo, AstBlock(functionBody().sourceElements().toAst()))
//is PropertySetterContext -> AstObjectSetter(setter().foo, propertySetParameterList().text, AstBlock(functionBody().sourceElements().toAst()))
	else -> { println("${this.text}, ${this.javaClass.canonicalName}");  throw Exception("invalid property assignment element type") }
}
fun PropertyNameContext.toAst() : Any = when {
	identifierName() != null		-> identifierName().text
	StringLiteral()?.text != null	-> StringLiteral().text
	numericLiteral() != null		-> numericLiteral().toAst()
	else -> { println("${this.text}, ${this.javaClass.canonicalName}");  throw Exception("invalid property name element type") }
}

fun FunctionDeclarationContext.toAst(): AstFuncDecl = AstFuncDecl(
	AstFuncExpr(
		Identifier().text, (formalParameterList()?.toAst()) ?: listOf(), AstBlock(functionBody().sourceElements().toAst())
	)
)
