/*
	Flowerbox
	Copyright 2018 Kayateia

	This code is licensed under the MIT license; please see LICENSE.md in the root of the project.
 */

package net.kayateia.flowerbox.common.cherry.runtime

import net.kayateia.flowerbox.common.cherry.objects.CherryNative
import net.kayateia.flowerbox.common.cherry.objects.CherryNativeCompanion
import net.kayateia.flowerbox.common.cherry.parser.*
import net.kayateia.flowerbox.common.cherry.runtime.library.Debug
import net.kayateia.flowerbox.common.cherry.runtime.library.Library
import net.kayateia.flowerbox.common.cherry.runtime.library.NativeImpl
import net.kayateia.flowerbox.common.cherry.runtime.library.NativeLibrary
import net.kayateia.flowerbox.common.cherry.runtime.scope.MapScope
import net.kayateia.flowerbox.common.cherry.runtime.scope.Scope
import net.kayateia.flowerbox.common.cherry.runtime.step.Step
import kotlin.coroutines.*
import kotlin.coroutines.intrinsics.*
import kotlin.coroutines.resume

class Runtime() {
	val scopeStack = ScopeStack()
	val lexicalStack = LexicalStack()
	var totalSteps = 0
	var maxSteps = 0
	var nextStep: Continuation<Unit>? = null

	var completed = false
	var result: Any? = null
	var exception: Throwable? = null

	var currentNamespace: String = ""
	var nsUsings: MutableList<String> = mutableListOf()

	val library = Library()
	val nativeLibrary = NativeLibrary()

	init {
		scopeStack.constScope.setLibrary(library)
		nativeLibrary.executeAllDecls(this)
	}

	/* fun testNative(member: String, self: HashMap<String, Value>?, capturedScope: Scope, params: ListValue): Value {
		println("testNative called - member=$member, self=$self, scope=$capturedScope, params=$params")
		return Value.box("test")
	}

	fun testFunc(implicits: Scope, args: ListValue): Value {
		println("Implicits: $implicits")
		println("Args: $args")
		println(lexicalStack)
		println(lexicalStack.stackTrace)
		return ConstValue(5)
	} */

	override fun toString(): String {
		exception?.printStackTrace()
		return "runtime: totalSteps ${totalSteps}, maxSteps ${maxSteps}, completed ${completed}, result ${result}, exc ${exception}\n${scopeStack}"
	}

	// Returns true if we've fully completed execution.
	fun execute(program: AstProgram, maxSteps: Int = -1): Boolean {
		if (nextStep != null)
			throw Exception("Attempt to start new execution before finishing previous one")

		this.maxSteps = maxSteps
		totalSteps = 0

		return beginExecute(suspend {
			Step.toStep(program).execute(this, program)
		})
	}

	private fun beginExecute(startFunc: suspend () -> Value): Boolean {
		nextStep = startFunc.createCoroutine(object : Continuation<Any?> {
			override val context: CoroutineContext
				get() = EmptyCoroutineContext

			override fun resumeWith(value: Result<Any?>) {
				result = value
				completed = true
			}
		})
		nextStep!!.resume(Unit)

		if (completed)
			nextStep = null

		return completed
	}

	fun executeMore(): Boolean {
		if (nextStep == null)
			throw Exception("Executing was never begun, can't continue")
		if (completed)
			throw Exception("Already completed, can't continue")

		nextStep!!.resume(Unit)

		if (completed)
			nextStep = null

		return completed
	}

	// Executes a single object creation with optional limit on steps. Returns true if we've fully completed execution.
	fun executeNew(className: String, args: List<Value> = listOf(), maxSteps: Int = -1): Boolean {
		if (nextStep != null)
			throw Exception("Attempt to start new execution before finishing previous one")

		val classObj = library.lookup(className, listOf())
		if (classObj == null || classObj !is ClassValue)
			throw Exception("can't find class $className (result was $classObj)")

		return beginExecute(suspend {
			classObj.construct(this, ListValue(args.toMutableList()))
		})
	}

	// Calls a method on an object. Returns true if we've fully completed execution.
	fun executeMethod(obj: ObjectValue, methodName: String, args: List<Value>, maxSteps: Int = -1): Boolean {
		if (nextStep != null)
			throw Exception("Attempt to start new execution before finishing previous one")

		return beginExecute(suspend {
			val method = obj.`class`.get(this, obj, methodName)
			if (method !is CValue)
				throw Exception("${obj.`class`.ast.name}.$methodName is not callable")
			method.call(this, ListValue(args.toMutableList()))
		})
	}

	// Takes a native object (and its companion object) that implement the CherryNative interface,
	// and returns a Cherry object wrapping it.
	fun wrapNative(obj: CherryNative, companion: CherryNativeCompanion): Value {
		val classObj = library.lookup(companion.className, listOf(companion.namespace))
		if (classObj == null || classObj !is ClassValue)
			throw Exception("can't find class ${companion.namespace}.${companion.className} (result was $classObj)")

		return ObjectValue(classObj, nativeObject = obj)
	}

	fun scopePush(scope: Scope = MapScope(scopeStack.top)): ScopeStackToken = ScopeStackToken(this, scopeStack.push(scope))
	fun scopePop(scope: Scope): Unit {
		val popped = scopeStack.pop()
		if (popped !== scope)
			throw Exception("scope stack push/pop mismatch")
	}
	val scope: Scope get() = scopeStack.top

	fun lexicalPush(node: AstNode): LexicalStackToken = LexicalStackToken(this, lexicalStack.push(node))
	fun lexicalPop(node: AstNode): Unit {
		val popped = lexicalStack.pop()
		if (popped !== node)
			throw Exception("lexical stack push/pop mismatch")
	}

	suspend fun stepAdd() {
		if (maxSteps < 0)
			return

		totalSteps++
		if (totalSteps >= maxSteps)
			return suspendCoroutine {
				nextStep = it
				COROUTINE_SUSPENDED
			}
	}
}
