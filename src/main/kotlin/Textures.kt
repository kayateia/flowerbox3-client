/*
	Flowerbox
	Copyright 2018 Kayateia

	This code is licensed under the MIT license; please see LICENSE.md in the root of the project.
 */

package net.kayateia.flowerbox.client

object Textures {
	val atlas = TextureAtlas(32)

	private val pathRoot: String = "./textures/"

	init {
		atlas.load(listOf(
				"grass_side.png",
				"grass_top.png",
				"dirt.png",
				"grass_side_snowed.png",
				"snow.png"
			).map({ p: String -> pathRoot + p })
		)
	}

	const val grassSide = 0
	const val grassTop = 1
	const val dirt = 2
	const val grassSnowedSide = 3
	const val snowTop = 4
}
