/*
	Flowerbox
	Copyright 2018 Kayateia

	Please see LICENSE.txt in the root of the project for more details.
 */

package net.kayateia.flowerbox.common.world.cherry

import net.kayateia.flowerbox.common.cherry.objects.CherryNative
import net.kayateia.flowerbox.common.cherry.objects.CherryNativeCompanion
import net.kayateia.flowerbox.common.cherry.objects.ExposedField
import net.kayateia.flowerbox.common.cherry.parser.Parser
import net.kayateia.flowerbox.common.cherry.runtime.*
import net.kayateia.flowerbox.common.cherry.runtime.scope.Scope

class NativeVector(var x: Double = 0.0, var y: Double = 0.0, var z: Double = 0.0, val changed: (NativeVector) -> Unit = {}) : CherryNative {
	companion object : CherryNativeCompanion, WorldCherryItem {
		override val namespace: String
			get() = "fb.world"
		override val className: String
			get() = "NativeVector"

		override val exposedFields: List<ExposedField>
			get() = listOf(
				ExposedField("x", true, true, true),
				ExposedField("y", true, true, true),
				ExposedField("z", true, true, true)
			)

		override suspend fun constructCompanion(runtime: Runtime, member: String, self: ObjectValue, scope: Scope, args: ListValue): Value {
			val obj = args.listValue[0] as ObjectValue
			obj.nativeObject = NativeVector()
			return NullValue()
		}

		private val code = """
			namespace fb.world;

			class NativeVector {
				public native get x() {}
				public native set x(val) {}
				public native get y() {}
				public native set y(val) {}
				public native get z() {}
				public native set z(val) {}
			}
		"""
		override val codeParsed = Parser().parse("<flowerbox>", code)
	}

	override fun getField(name: String): Any? = when (name) {
		"x" -> x
		"y" -> y
		"z" -> z
		else -> throw Exception("Unsupported Vector member $name")
	}

	override fun setField(name: String, value: Any?) {
		when (name) {
			"x" -> x = Coercion.toNum(value)
			"y" -> y = Coercion.toNum(value)
			"z" -> z = Coercion.toNum(value)
		}
		changed(this)
	}
}
