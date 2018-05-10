/*
	Flowerbox
	Copyright 2018 Kayateia

	This code is licensed under the MIT license; please see LICENSE.md in the root of the project.
 */

package net.kayateia.flowerbox.common.cherry.objects

import net.kayateia.flowerbox.common.cherry.runtime.Runtime

class CherryPropertyBag(val runtime: Runtime, val namespace: String, val className: String, val propertyNames: List<String>) : CherryNative {
	val bag: HashMap<String, Any?> = HashMap()

	override val exposedFields: List<ExposedField>
		get() = propertyNames.map { ExposedField(it, true, true, true) }

	override fun getField(name: String): Any? = bag[name]
	override fun setField(name: String, value: Any?) {
		bag[name] = value
	}

	init {
		setupCherry(runtime, namespace, className)
	}
}
