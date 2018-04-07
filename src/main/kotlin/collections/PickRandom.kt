/*
	Flowerbox
	Copyright 2018 Kayateia

	This code is licensed under the MIT license; please see LICENSE.md in the root of the project.
 */

package net.kayateia.flowerbox.client.collections

import java.util.Random

object PickRandom {
	val random = Random()
}

fun <E> List<E>.pickRandom() = this[PickRandom.random.nextInt(this.size)]
