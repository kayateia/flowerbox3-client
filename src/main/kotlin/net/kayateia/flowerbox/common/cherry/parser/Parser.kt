/*
	Flowerbox
	Copyright 2018 Kayateia

	Please see LICENSE.txt in the root of the project for more details.
 */

package net.kayateia.flowerbox.common.cherry.parser

import net.kayateia.flowerbox.common.cherry.antlr.*
import net.kayateia.flowerbox.common.cherry.runtime.Runtime
import net.kayateia.flowerbox.common.cherry.runtime.library.Debug
import net.kayateia.flowerbox.common.cherry.runtime.library.Math
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

namespace foo.bar.baz;

// using c.d.*;

class testbase {
	public baseA = 15;
	public static baseC = 12;

	public native testNative(a, b) {}
	public native get testNativeGet() {}

	public init(a) {
		sys.dbg.println("testbase constructor!", a, self);
	}

	public randomMethod() {
		sys.dbg.println("randomMethod called");
		return "fooz";
	}

	public static randomStatic() {
		sys.dbg.println("randomstatic called");
	}

	public get woot() {
		return "basewoot";
	}

	public static set basefooz(val) {
		sys.dbg.println("basefooz was called", val);
	}
}

class test : testbase {
	external foo_texture;
	public a = 10, b = 5;
	public static c = 6;

	public init(d, e) {
		base.init(20);
		sys.dbg.println("Constructor!", d, e, self);
	}

	public testMe() {
		sys.dbg.println("testMe was called.");
		self.furtherTest();
	}

	private furtherTest() {
		sys.dbg.println("furtherTest was called on", self);
	}

	protected foo(a, b, c) {
		return c;
	}

	public get woot() {
		return 10;
	}
	private static set fooz(val) {
		sys.dbg.println("fooz was called with", val);
	}
}
var testobj = new test("a", 5);
testobj.testMe();
sys.dbg.println("woot is", testobj.woot);
test.fooz = "fooz test value";
sys.dbg.println("test.c is", test.c);

testobj.randomMethod();
test.randomStatic();
test.basefooz = "set base fooz";

testobj.testNative(1, 2, 3);
sys.dbg.println("testNativeGet is", testobj.testNativeGet);

var testdict = {
	"foo": 5, "bar": 10, 15: "fooz", doIt: function() { sys.dbg.println("Inside doIt!", self); }
};
sys.dbg.println("foo is", testdict.foo);
sys.dbg.println("15 is", testdict[15]);
testdict.doIt();

var list = [1,2,3];
list[1] = "fooz";
var listread = list[2];

try {
	var tmp = function() {
		sys.dbg.println("throwing now!");
		throw "test throw";
	};
	tmp();
	sys.dbg.println("past func call");
} catch (caught) {
	sys.dbg.println(caught);
} finally {
	sys.dbg.println("finally!");
}

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

var g = 5;
var h = g++;
var i = ++g;

			""")
			val runtime = Runtime()
			val rv = runtime.execute(ast, 10000)
			println("$runtime")
			// println("${runtime.completed} ${runtime.result} ${rv}")
		}
	}
}
