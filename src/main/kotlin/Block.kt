/*
	Flowerbox
	Copyright 2018 Kayateia

	This code is licensed under the MIT license; please see LICENSE.md in the root of the project.
 */

package net.kayateia.flowerbox.client

data class TextureSet(val top: TextureCoord,
						val bottom: TextureCoord,
						val left: TextureCoord,
						val right: TextureCoord,
						val front: TextureCoord,
						val back: TextureCoord)

object TextureSets {
	private val atlas by lazy { Textures.atlas }
	val grass: TextureSet by lazy {
		TextureSet(top = atlas.coordsOf(Textures.grassTop),
				bottom = atlas.coordsOf(Textures.dirt),
				left = atlas.coordsOf(Textures.grassSide),
				right = atlas.coordsOf(Textures.grassSide),
				front = atlas.coordsOf(Textures.grassSide),
				back = atlas.coordsOf(Textures.grassSide)
		)
	}

	val snow: TextureSet by lazy {
		TextureSet(top = atlas.coordsOf(Textures.snowTop),
				bottom = atlas.coordsOf(Textures.dirt),
				left = atlas.coordsOf(Textures.grassSnowedSide),
				right = atlas.coordsOf(Textures.grassSnowedSide),
				front = atlas.coordsOf(Textures.grassSnowedSide),
				back = atlas.coordsOf(Textures.grassSnowedSide)
		)
	}

	val dirt: TextureSet by lazy {
		val dirtTxr = atlas.coordsOf(Textures.dirt)
		TextureSet(top = dirtTxr,
				bottom = dirtTxr,
				left = dirtTxr,
				right = dirtTxr,
				front = dirtTxr,
				back = dirtTxr
		)
	}
}

class Block(val blockType: Int) {
	companion object {
		const val air: Int = 0
		const val grass: Int = 1
		const val dirt: Int = 2
		const val snow: Int = 3

		private val textureSets = arrayOf(
				null,
				TextureSets.grass,
				TextureSets.dirt,
				TextureSets.snow
		)
	}

	val textureSet: TextureSet? by lazy {
		if (blockType <= 3)
			textureSets[blockType]
		else
			null
	}

	val isFilled: Boolean get() = blockType != air
}
