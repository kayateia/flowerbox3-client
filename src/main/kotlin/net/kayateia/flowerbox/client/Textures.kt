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
				"grass_top2.png",
				"grass_top3.png",
				"dirt.png",
				"grass_side_snowed.png",
				"snow.png",
				"water.png"
			).map({ p: String -> pathRoot + p })
		)
	}

	const val grassSide = 0
	const val grassTop1 = 1
	const val grassTop2 = 2
	const val grassTop3 = 3
	const val dirt = 4
	const val grassSnowedSide = 5
	const val snowTop = 6
	const val water = 7

	val aoTxr: SingleTexture = SingleTexture("./textures/aotxrs.png")
	// val sdTxr: SingleTexture = SingleTexture("./textures/sidetxrs.png")
}
