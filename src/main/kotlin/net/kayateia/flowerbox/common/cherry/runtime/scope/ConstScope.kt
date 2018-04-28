/*
	Flowerbox
	Copyright 2018 Kayateia

	This code is licensed under the MIT license; please see LICENSE.md in the root of the project.
 */

package net.kayateia.flowerbox.common.cherry.runtime.scope

import net.kayateia.flowerbox.common.cherry.runtime.library.Library

open class ConstScope(override val parent: Scope? = null) : MapScope(parent) {
	override fun set(name: String, value: Any?) {
		/* should throw an error here */
	}
	override fun setLocal(name: String, value: Any?) {
		/* should throw an error here */
	}

	fun setConstant(name: String, value: Any?) = super.setLocal(name, value)

	fun setLibrary(library: Library) {
		library.rootPackages.map.forEach { super.setLocal(it.key, it.value) }
	}
}
