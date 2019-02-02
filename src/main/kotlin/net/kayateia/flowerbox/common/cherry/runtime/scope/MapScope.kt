/*
	Flowerbox
	Copyright 2018 Kayateia

	This code is licensed under the GNU Public License version 3 or higher.
	Please see LICENSE.txt in the root of the project for more details.
 */

package net.kayateia.flowerbox.common.cherry.runtime.scope

open class MapScope(override val parent: Scope? = null) : Scope {
	private val vars = HashMap<String, Any?>()

	override fun setLocal(name: String, value: Any?) { vars[name] = value }
	override fun getLocal(name: String) = vars[name]
	override fun hasLocal(name: String) = vars.containsKey(name)

	override fun get(name: String): Any? = if (hasLocal(name)) getLocal(name) else parent?.get(name)
	override fun set(name: String, value: Any?) {
		if (!hasLocal(name) && has(name)) {
			parent?.set(name, value)
		} else {
			setLocal(name, value)
		}
	}
	override fun has(name: String): Boolean = if (hasLocal(name)) true else (parent?.has(name) ?: false)

	override fun toString(): String = "mapscope: ${vars}"
}
