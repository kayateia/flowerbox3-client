/*
	Flowerbox
	Copyright 2018 Kayateia

	This code is licensed under the MIT license; please see LICENSE.md in the root of the project.
 */

package net.kayateia.flowerbox.common.cherry.parser

import net.kayateia.flowerbox.common.cherry.antlr.CherryParser.*

fun ProgramContext.toAst(): AstProgram = AstProgram(AstLoc.from(start), sourceElements().toAst())

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
	is BlockStmtContext		-> AstBlock(AstLoc.from(start), block().statementList().statement().map { it.toAst() })
	is VarStmtContext		-> AstVarStmt(AstLoc.from(start), variableStatement().variableDeclarationList().variableDeclaration().map { it.toAst() })
	is EmptyStmtContext		-> AstEmptyStmt(AstLoc.from(start))
	is ExprStmtContext		-> AstExprStmt(AstLoc.from(start), expressionStatement().expressionSequence().toAst())
	is IfStmtContext		-> ifStatement().toAst()
	is IterStmtContext		-> iterationStatement().toAst()
	is ContStmtContext		-> AstContinueStmt(AstLoc.from(start))
	is BreakStmtContext		-> AstBreakStmt(AstLoc.from(start))
	is RetStmtContext		-> AstReturnStmt(AstLoc.from(start), returnStatement().expressionSequence().toAst())
	is WithStmtContext		-> AstWithStmt(AstLoc.from(start), withStatement().expressionSequence().toAst(), withStatement().statement().toAst())
	is LabelStmtContext		-> throw Exception("not supported")
	is SwitchStmtContext	-> TODO()
	is ThrowStmtContext		-> AstThrowStmt(AstLoc.from(start), throwStatement().expressionSequence().toAst())
	is TryStmtContext		-> TODO()
	is DebugStmtContext		-> throw Exception("not supported")
	else -> { println("${this.text}, ${this.javaClass.canonicalName}");  throw Exception("invalid statement element type") }
}

fun IfStatementContext.toAst(): AstIfStmt = AstIfStmt(AstLoc.from(start), expressionSequence().toAst(), statement(0).toAst(), statement(1).toAst())

fun IterationStatementContext.toAst() : AstStatement = when(this) {
	is DoStatementContext		-> AstDoWhileStmt(AstLoc.from(start), statement().toAst(), expressionSequence().toAst())
	is WhileStatementContext	-> AstWhileStmt(AstLoc.from(start), expressionSequence().toAst(), statement().toAst())
	is ForStatementContext		-> AstForSeq(AstLoc.from(start), expressionSequence().get(0).toAst(), expressionSequence().get(1).toAst(), expressionSequence().get(2).toAst(), statement().toAst())
	is ForVarStatementContext	-> AstForVarSeq(AstLoc.from(start), variableDeclarationList().toAst(), expressionSequence().get(0).toAst(), expressionSequence().get(1).toAst(), statement().toAst())
	is ForInStatementContext	-> AstForIn(AstLoc.from(start), singleExpression().toAst(), expressionSequence().toAst(), statement().toAst())
	is ForVarInStatementContext	-> AstForVarIn(AstLoc.from(start), variableDeclaration().toAst(), expressionSequence().toAst(), statement().toAst())
	else -> { println("${this.text}, ${this.javaClass.canonicalName}");  throw Exception("invalid iteration element type") }
}

fun VariableDeclarationListContext.toAst(): List<AstVarDecl> = variableDeclaration().map { it.toAst() }
fun VariableDeclarationContext.toAst(): AstVarDecl = AstVarDecl(AstLoc.from(start), Identifier().text, initialiser()?.singleExpression()?.toAst())

fun ExpressionSequenceContext.toAst() : List<AstExpr> = this.singleExpression().map { it.toAst() }
fun SingleExpressionContext.toAst(): AstExpr = when(this) {
	is FunctionExpressionContext			-> AstFuncExpr(AstLoc.from(start), Identifier()?.text, formalParameterList().toAst(), AstBlock(AstLoc.from(start), functionBody().sourceElements().toAst()))
	is MemberIndexExpressionContext			-> AstIndexExpr(AstLoc.from(start), singleExpression().toAst(), expressionSequence().toAst())
	is MemberDotExpressionContext			-> AstDotExpr(AstLoc.from(start), singleExpression().toAst(), identifierName().text)
	is ArgumentsExpressionContext			-> AstCallExpr(AstLoc.from(start), singleExpression().toAst(), arguments().toAst())
	is NewExpressionContext					-> AstNewExpr(AstLoc.from(start), singleExpression().toAst(), arguments().toAst())
	is PostIncrementExpressionContext		-> AstPostExpr(AstLoc.from(start), getChild(1).text, singleExpression().toAst())
	is PostDecreaseExpressionContext		-> AstPostExpr(AstLoc.from(start), getChild(1).text, singleExpression().toAst())
	is DeleteExpressionContext				-> AstDeleteExpr(AstLoc.from(start), singleExpression().toAst())
	is VoidExpressionContext				-> AstVoidExpr(AstLoc.from(start), singleExpression().toAst())
	is TypeofExpressionContext				-> AstTypeofExpr(AstLoc.from(start), singleExpression().toAst())
	is PreIncrementExpressionContext		-> AstPreExpr(AstLoc.from(start), getChild(0).text, singleExpression().toAst())
	is PreDecreaseExpressionContext			-> AstPreExpr(AstLoc.from(start), getChild(0).text, singleExpression().toAst())
	is UnaryPlusExpressionContext			-> AstUnaryExpr(AstLoc.from(start), getChild(0).text, singleExpression().toAst())
	is UnaryMinusExpressionContext			-> AstUnaryExpr(AstLoc.from(start), getChild(0).text, singleExpression().toAst())
	is BitNotExpressionContext				-> AstUnaryExpr(AstLoc.from(start), getChild(0).text, singleExpression().toAst())
	is NotExpressionContext					-> AstUnaryExpr(AstLoc.from(start), getChild(0).text, singleExpression().toAst())
	is MultiplicativeExpressionContext		-> AstBinaryExpr(AstLoc.from(start), singleExpression(0).toAst(), getChild(1).text, singleExpression(1).toAst())
	is AdditiveExpressionContext			-> AstBinaryExpr(AstLoc.from(start), singleExpression(0).toAst(), getChild(1).text, singleExpression(1).toAst())
	is BitShiftExpressionContext			-> AstBinaryExpr(AstLoc.from(start), singleExpression(0).toAst(), getChild(1).text, singleExpression(1).toAst())
	is RelationalExpressionContext			-> AstBinaryExpr(AstLoc.from(start), singleExpression(0).toAst(), getChild(1).text, singleExpression(1).toAst())
	is InstanceofExpressionContext			-> AstBinaryExpr(AstLoc.from(start), singleExpression(0).toAst(), getChild(1).text, singleExpression(1).toAst())
	is InExpressionContext					-> AstBinaryExpr(AstLoc.from(start), singleExpression(0).toAst(), getChild(1).text, singleExpression(1).toAst())
	is EqualityExpressionContext			-> AstBinaryExpr(AstLoc.from(start), singleExpression(0).toAst(), getChild(1).text, singleExpression(1).toAst())
	is BitAndExpressionContext				-> AstBinaryExpr(AstLoc.from(start), singleExpression(0).toAst(), getChild(1).text, singleExpression(1).toAst())
	is BitXOrExpressionContext				-> AstBinaryExpr(AstLoc.from(start), singleExpression(0).toAst(), getChild(1).text, singleExpression(1).toAst())
	is BitOrExpressionContext				-> AstBinaryExpr(AstLoc.from(start), singleExpression(0).toAst(), getChild(1).text, singleExpression(1).toAst())
	is LogicalAndExpressionContext			-> AstLazyBinaryExpr(AstLoc.from(start), singleExpression(0).toAst(), getChild(1).text, singleExpression(1).toAst())
	is LogicalOrExpressionContext			-> AstLazyBinaryExpr(AstLoc.from(start), singleExpression(0).toAst(), getChild(1).text, singleExpression(1).toAst())
	is TernaryExpressionContext				-> AstTernaryExpr(AstLoc.from(start), singleExpression(0).toAst(), singleExpression(1).toAst(), singleExpression(2).toAst())
	is AssignmentExpressionContext			-> AstBinaryExpr(AstLoc.from(start), singleExpression(0).toAst(), getChild(1).text, singleExpression(1).toAst())
	is AssignmentOperatorExpressionContext	-> AstBinaryExpr(AstLoc.from(start), singleExpression(0).toAst(), assignmentOperator().text, singleExpression(1).toAst())
	is ThisExpressionContext				-> AstThisExpr(AstLoc.from(start))
	is IdentifierExpressionContext			-> AstIdExpr(AstLoc.from(start), Identifier().text)
	is LiteralExpressionContext				-> AstLiteralExpr(AstLoc.from(start), literal().toAst())
	is ArrayLiteralExpressionContext		-> AstArrayExpr(AstLoc.from(start), arrayLiteral().toAst())
// TODO
// is ObjectLiteralExpressionContext -> AstObjectExpr(objectLiteral().toAst())
	is ParenthesizedExpressionContext		-> AstExprListExpr(AstLoc.from(start), expressionSequence().toAst())
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
	return AstObjectExpr(AstLoc.from(start), map)
}
fun PropertyAssignmentContext.toAst() : AstObjectProperty = when (this) {
	is PropertyExpressionAssignmentContext	-> AstObjectAssignment(AstLoc.from(start), propertyName().toAst(), singleExpression().toAst())
// TODO
//is PropertyGetterContext -> AstObjectGetter(getter().foo, AstBlock(functionBody().sourceElements().toAst()))
//is PropertySetterContext -> AstObjectSetter(setter().foo, propertySetParameterList().text, AstBlock(functionBody().sourceElements().toAst()))
	is PropertyGetterContext -> TODO()
	is PropertySetterContext -> TODO()
	else -> { println("${this.text}, ${this.javaClass.canonicalName}");  throw Exception("invalid property assignment element type") }
}
fun PropertyNameContext.toAst() : Any = when {
	identifierName() != null		-> identifierName().text
	StringLiteral()?.text != null	-> StringLiteral().text
	numericLiteral() != null		-> numericLiteral().toAst()
	else -> { println("${this.text}, ${this.javaClass.canonicalName}");  throw Exception("invalid property name element type") }
}

fun FunctionDeclarationContext.toAst(): AstFuncDecl = AstFuncDecl(AstLoc.from(start),
		AstFuncExpr(
		AstLoc.from(start),
		Identifier().text, (formalParameterList()?.toAst())
		?: listOf(), AstBlock(AstLoc.from(start), functionBody().sourceElements().toAst())
	)
)
