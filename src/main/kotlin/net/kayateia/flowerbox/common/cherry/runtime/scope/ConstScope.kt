/*
	Flowerbox
	Copyright 2018 Kayateia

	This code is licensed under the GNU Public License version 3 or higher.
	Please see LICENSE.txt in the root of the project for more details.
 */

package net.kayateia.flowerbox.common.cherry.runtime.scope

import net.kayateia.flowerbox.common.cherry.runtime.library.Library

open class ConstScope(override val parent: Scope? = null) : MapScope(parent) {
	override fun set(name: String, value: Any?) {
		if (hasLocal(name))
			throw Exception("can't write to const value ${name}")
	}
	override fun setLocal(name: String, value: Any?) {
		throw Exception("can't add value to a const scope (${name})")
	}

	fun setConstant(name: String, value: Any?) = super.setLocal(name, value)

	fun setLibrary(library: Library) {
		library.rootPackages.map.forEach { super.setLocal(it.key, it.value) }
	}
}
