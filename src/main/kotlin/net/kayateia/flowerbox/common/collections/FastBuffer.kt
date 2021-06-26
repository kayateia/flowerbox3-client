/*
	Flowerbox
	Copyright 2018 Kayateia

	Please see LICENSE.txt in the root of the project for more details.
 */

package net.kayateia.flowerbox.common.collections

// I really wish I understood Kotlin enough to combine these into a working generic, but I
// guess for now this will have to do.
class FastFloatBuffer(initialSize: Int) {
	private val array: FloatArray = FloatArray(initialSize)
	private var idx: Int = 0

	fun addAll(vararg vs: Float) {
		for (v in vs)
			array[idx++] = v
	}

	val length get() = idx

	fun result(): FloatArray {
		return array.copyOfRange(0, length)
	}
}

class FastShortBuffer(initialSize: Int) {
	private val array: ShortArray = ShortArray(initialSize)
	private var idx: Int = 0

	fun addAll(vararg vs: Int) {
		for (v in vs)
			array[idx++] = v.toShort()
	}

	val length get() = idx

	fun result(): ShortArray {
		return array.copyOfRange(0, length)
	}
}
