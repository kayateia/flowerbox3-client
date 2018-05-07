/*
	Flowerbox
	Copyright 2018 Kayateia

	This code is licensed under the MIT license; please see LICENSE.md in the root of the project.
 */

package net.kayateia.flowerbox.common.cherry.runtime

import net.kayateia.flowerbox.common.cherry.parser.AstFuncExpr
import net.kayateia.flowerbox.common.cherry.runtime.library.NativeImpl
import net.kayateia.flowerbox.common.cherry.runtime.scope.ConstScope
import net.kayateia.flowerbox.common.cherry.runtime.scope.MapScope
import net.kayateia.flowerbox.common.cherry.runtime.scope.Scope
import net.kayateia.flowerbox.common.cherry.runtime.step.Step

// Represents a callable non-primitive value in the runtime.
interface CValue : Value {
	suspend fun call(runtime: Runtime, args: ListValue): Value
}

class FuncValue(val funcNode: AstFuncExpr, val capturedScope: Scope) : CValue {
	override suspend fun call(runtime: Runtime, args: ListValue): Value {
		val paramScope = MapScope(capturedScope)
		paramScope.setLocal("arguments", args)

		funcNode.params?.zip(args.listValue)?.forEach {
			paramScope.setLocal(it.first, it.second)
		}
		funcNode.params?.forEach {
			if (!paramScope.hasLocal(it))
				paramScope.setLocal(it, NullValue())
		}

		runtime.scopePush(paramScope).use {
			val rv = Step.exec(runtime, funcNode.body)
			return when (rv) {
				is ReturnValue -> Value.root(runtime, rv.returnValue)
				is ThrownValue -> rv
				else -> Value.root(runtime, rv)
			}
		}
	}
	override fun toString(): String = "FuncValue(${funcNode.id}(${funcNode.params?.fold("", {a,b -> "$a,$b"})}))"

	fun newSelf(self: Value): FuncValue {
		val funcScope = ConstScope(capturedScope)
		funcScope.setConstant("self", self)
		return FuncValue(funcNode, funcScope)
	}
}

class NativeValue(val native: NativeImpl, val capturedScope: Scope) : CValue {
	override suspend fun call(runtime: Runtime, args: ListValue): Value {
		val self = capturedScope.get("self")
		val selfMap = if (self is ObjectValue)
				self.map
			else
				null

		return native.impl(runtime, native.memberName, selfMap, capturedScope, args)
	}
	override fun toString(): String = "Native(${native.namespace}.${native.className}.${native.memberName})"
}

