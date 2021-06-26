/*
	Flowerbox
	Copyright 2018 Kayateia

	Please see LICENSE.txt in the root of the project for more details.
 */

package net.kayateia.flowerbox.common.cherry.objects

import net.kayateia.flowerbox.common.cherry.parser.AstProgram
import net.kayateia.flowerbox.common.cherry.runtime.*
import net.kayateia.flowerbox.common.cherry.runtime.library.NativeImpl
import net.kayateia.flowerbox.common.cherry.runtime.scope.Scope

data class ExposedField(val name: String, val primify: Boolean, val getter: Boolean, val setter: Boolean)
data class ExposedMethod(val name: String, val primify: Boolean)

interface CherryNativeCompanion {
	val exposedFields: List<ExposedField>
		get() = listOf()
	val exposedMethods: List<ExposedMethod>
		get() = listOf()

	val namespace: String
	val className: String

	fun cherrySetup(runtime: Runtime) {
		for (fn in exposedFields) {
			// In this context, 'self' is always non-null.
			if (fn.getter)
				runtime.nativeLibrary.register(
					NativeImpl(namespace, className, "${ClassSpecialNames.GetterPrefix}${fn.name}", { rt, mem, self, scope, args -> getFieldTranslate(self!!, fn, rt) })
				)

			if (fn.setter)
				runtime.nativeLibrary.register(
					NativeImpl(namespace, className, "${ClassSpecialNames.SetterPrefix}${fn.name}", { rt, mem, self, scope, args -> setFieldTranslate(self!!, fn, rt, args) })
				)
		}

		for (m in exposedMethods) {
			// Same as above, in this context, 'self' is always non-null.
			runtime.nativeLibrary.register(
				NativeImpl(namespace, className, m.name, { rt, mem, self, scope, args -> callMethodTranslate(self!!, m, rt, args) })
			)
		}

		// Same as above, in this context, 'self' is always non-null.
		runtime.nativeLibrary.register(
			NativeImpl(namespace, className, ClassSpecialNames.NativeInitMethod, { rt, mem, self, scope, args -> constructCompanion(rt, mem, self!!, scope, args) })
		)
	}

	suspend fun constructCompanion(runtime: Runtime, member: String, self: ObjectValue, scope: Scope, args: ListValue): Value

	suspend fun getFieldTranslate(self: ObjectValue, field: ExposedField, runtime: Runtime): Value {
		val nativeObject = self.nativeObject ?: throw Exception("Field ${field.name} was marked native but doesn't appear to have a native object.")
		return nativeObject.getFieldTranslate(field, runtime)
	}

	suspend fun setFieldTranslate(self: ObjectValue, field: ExposedField, runtime: Runtime, args: ListValue): Value {
		val nativeObject = self.nativeObject ?: throw Exception("Field ${field.name} was marked native but doesn't appear to have a native object.")
		return nativeObject.setFieldTranslate(field, runtime, args)
	}

	suspend fun callMethodTranslate(self: ObjectValue, member: ExposedMethod, runtime: Runtime, args: ListValue): Value {
		val nativeObject = self.nativeObject ?: throw Exception("Method ${member.name} was marked native but doesn't appear to have a native object.")
		return nativeObject.callMethodTranslate(member, runtime, args)
	}
}

interface CherryNative {
	fun getField(name: String): Any? = throw Exception("Getting field $name is not supported")
	fun setField(name: String, value: Any?): Unit = throw Exception("Setting field $name is not supported")

	fun callMethod(name: String, args: List<Any?>): Any? = throw Exception("Calling method $name is not supported")

	suspend fun getFieldTranslate(field: ExposedField, runtime: Runtime): Value {
		// TODO - catch Kotlin exceptions and re-throw as Cherry
		//try {
			val value = getField(field.name)
			return Value.box(value)
		//} catch (e: Exception) {
		//}
	}

	suspend fun setFieldTranslate(field: ExposedField, runtime: Runtime, args: ListValue): Value {
		// TODO - catch Kotlin exceptions and re-throw as Cherry
		//try {
			if (args.listValue.count() != 1)
				throw Exception("invalid parameters to setter ($args)")

			val prim = if (field.primify)
					Value.prim(runtime, args.listValue[0])
				else
					args.listValue[0]

			setField(field.name, prim)

			return NullValue()
		//} catch (e: Exception) {
		//}
	}

	suspend fun callMethodTranslate(method: ExposedMethod, runtime: Runtime, args: ListValue): Value {
		// TODO - catch Kotlin exceptions and re-throw as Cherry
		//try {
			val primArgs = args.listValue.map {
				if (method.primify)
					Value.prim(runtime, it)
				else
					it
			}
			val result = callMethod(method.name, primArgs)
			return Value.box(result)
		//} catch (e: Exception) {
		//}
	}
}
