/*
	Flowerbox
	Copyright 2018 Kayateia

	This code is licensed under the GNU Public License version 3 or higher.
	Please see LICENSE.txt in the root of the project for more details.
 */

package net.kayateia.flowerbox.common.cherry.runtime.scope

interface Scope {
	fun setLocal(name: String, value: Any?)
	fun getLocal(name: String): Any?
	fun hasLocal(name: String): Boolean

	fun get(name: String): Any?
	fun set(name: String, value: Any?)
	fun has(name: String): Boolean

	val parent: Scope?
}
