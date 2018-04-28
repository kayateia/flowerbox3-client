/*
	Flowerbox
	Copyright 2018 Kayateia

	This code is licensed under the MIT license; please see LICENSE.md in the root of the project.
 */

package net.kayateia.flowerbox.common.cherry.parser

import net.kayateia.flowerbox.common.cherry.antlr.*
import net.kayateia.flowerbox.common.cherry.runtime.Runtime
import org.antlr.v4.runtime.*
import org.antlr.v4.runtime.Parser
import org.antlr.v4.runtime.atn.ATNConfigSet
import org.antlr.v4.runtime.dfa.DFA
import java.util.*

class Parser {
	private val functionStack = Stack<AstFuncExpr>()

	private val errorListener = object : ANTLRErrorListener {
		override fun syntaxError(recognizer: Recognizer<*, *>?, offendingSymbol: Any?, line: Int, charPositionInline: Int, msg: String?, p5: RecognitionException?) {
			println("Error $msg at $line,$charPositionInline")
		}

		override fun reportAmbiguity(p0: Parser?, p1: DFA?, p2: Int, p3: Int, p4: Boolean, p5: BitSet?, p6: ATNConfigSet?) { }
		override fun reportContextSensitivity(p0: Parser?, p1: DFA?, p2: Int, p3: Int, p4: Int, p5: ATNConfigSet?) { }
		override fun reportAttemptingFullContext(p0: Parser?, p1: DFA?, p2: Int, p3: Int, p4: BitSet?, p5: ATNConfigSet?) { }
	}

	fun functionPush(func: AstFuncExpr) = functionStack.push(func)
	fun functionPop(func: AstFuncExpr) {
		val popped = functionStack.pop()
		if (popped !== func)
			throw Exception("Popped function was not the same as pushed function (func = $func, popped = $popped)")
	}
	val functionTop: AstFuncExpr? get() = if (functionStack.isEmpty()) null else functionStack.peek()

	private var _module: AstModule = AstModule("")
	val module: AstModule get() = _module

	fun parse(moduleName: String, text: String): AstProgram {
		_module = AstModule(moduleName)

		val lexer = CherryLexer(ANTLRInputStream(text))
		lexer.removeErrorListeners()
		lexer.addErrorListener(errorListener)
		val parser = CherryParser(CommonTokenStream(lexer))
		parser.addErrorListener(errorListener)

		val antlrRoot = parser.program()
		val ast = antlrRoot.toAst(this)

		println(ast)
		return ast
	}

	companion object {
		fun test() {
/*			val ast = Parser().parse("""
function factorial(n) {
	if (n == 0)
		return 1;
	else
		return n * factorial(n-1);
}
// var obj = { a: 10, b: factorial(10) }
// var arr = ["foo", 10, "bar", 0x10]
var i;
document.clear();
for (i = 0; i <= 16; i++)
	document.write(i + "! = " + factorial(i) + "<br />");
		""") */
			val ast = Parser().parse("<inline>", """
var f = 5;
switch (f) {
	case "a":
		break;
	case 5:
		f++;
		// break;
	default:
		f = "fooz:" + f;
		break;
}

function factorial(n) {
	if (n == 0) {
		testfunc();
		return 1;
	} else
		return n * factorial(n-1);
}
function test(n) {
	return n + 3
}
var j = test(2);
var k = factorial(4);
				var i = (5 * 10 + 5) - 20;
				i = i + 1;

var m = 0;
for (var l=0; l<10; l++) {
	m = m + 10;
}
var p = testfunc();

var q = sys.math.sin(0.5);

m = 0;
while (m < 5)
	m++;
var n = 0;
do {
	n++;
} while (n < 5);

var a = 0;
while (a < 5) {
	if (a == 3)
		break;
	else
		a++;
}

var b = "";
for (var c=0; c<10; c++) {
	if (c == 4)
		continue;
	b = b + c;
}

			""")
			val runtime = Runtime(ast)
			val rv = runtime.execute(10000)
			println("$runtime")
			// println("${runtime.completed} ${runtime.result} ${rv}")
		}
	}
}
