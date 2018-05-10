/*
	Flowerbox
	Copyright 2018 Kayateia

	This code is licensed under the MIT license; please see LICENSE.md in the root of the project.
 */

package net.kayateia.flowerbox.common.cherry.objects

import net.kayateia.flowerbox.common.cherry.runtime.ListValue
import net.kayateia.flowerbox.common.cherry.runtime.NullValue
import net.kayateia.flowerbox.common.cherry.runtime.Runtime
import net.kayateia.flowerbox.common.cherry.runtime.Value
import net.kayateia.flowerbox.common.cherry.runtime.library.NativeImpl

data class ExposedField(val name: String, val primify: Boolean, val getter: Boolean, val setter: Boolean)
data class ExposedMethod(val name: String, val primify: Boolean)

interface CherryNative {
	val exposedFields: List<ExposedField>
		get() = listOf()
	val exposedMethods: List<ExposedMethod>
		get() = listOf()

	fun getField(name: String): Any? = throw Exception("Getting field $name is not supported")
	fun setField(name: String, value: Any?): Unit = throw Exception("Setting field $name is not supported")

	fun callMethod(name: String, args: List<Any?>): Any? = throw Exception("Calling method $name is not supported")

	fun setupCherry(runtime: Runtime, namespace: String, className: String) {
		for (fn in exposedFields) {
			if (fn.getter)
				runtime.nativeLibrary.register(
					NativeImpl(namespace, className, "get::${fn.name}", { rt, mem, self, scope, args -> getFieldTranslate(fn, rt) })
				)

			if (fn.setter)
				runtime.nativeLibrary.register(
					NativeImpl(namespace, className, "set::${fn.name}", { rt, mem, self, scope, args -> setFieldTranslate(fn, rt, args) })
				)
		}

		for (m in exposedMethods) {
			runtime.nativeLibrary.register(
				NativeImpl(namespace, className, m.name, { rt, mem, self, scope, args -> callMethodTranslate(m, rt, args) })
			)
		}
	}

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
