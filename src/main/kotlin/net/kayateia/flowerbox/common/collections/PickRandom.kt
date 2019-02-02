/*
	Flowerbox
	Copyright 2018 Kayateia

	This code is licensed under the GNU Public License version 3 or higher.
	Please see LICENSE.txt in the root of the project for more details.
 */

package net.kayateia.flowerbox.common.collections

import java.util.Random

object PickRandom {
	val random = Random()
}

fun <E> List<E>.pickRandom() = this[PickRandom.random.nextInt(this.size)]
