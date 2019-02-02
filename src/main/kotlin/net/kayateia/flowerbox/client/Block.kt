/*
	Flowerbox
	Copyright 2018 Kayateia

	This code is licensed under the GNU Public License version 3 or higher.
	Please see LICENSE.txt in the root of the project for more details.
 */

package net.kayateia.flowerbox.client

import net.kayateia.flowerbox.common.collections.pickRandom

data class TextureSet(val top: TextureQuad,
						val bottom: TextureQuad,
						val left: TextureQuad,
						val right: TextureQuad,
						val front: TextureQuad,
						val back: TextureQuad)

object TextureSets {
	private val atlas by lazy { Textures.atlas }
	val grass1: TextureSet by lazy {
		TextureSet(top = atlas.coordsOf(Textures.grassTop1),
				bottom = atlas.coordsOf(Textures.dirt),
				left = atlas.coordsOf(Textures.grassSide),
				right = atlas.coordsOf(Textures.grassSide),
				front = atlas.coordsOf(Textures.grassSide),
				back = atlas.coordsOf(Textures.grassSide)
		)
	}

	val grass2: TextureSet by lazy {
		TextureSet(top = atlas.coordsOf(Textures.grassTop2),
				bottom = atlas.coordsOf(Textures.dirt),
				left = atlas.coordsOf(Textures.grassSide),
				right = atlas.coordsOf(Textures.grassSide),
				front = atlas.coordsOf(Textures.grassSide),
				back = atlas.coordsOf(Textures.grassSide)
		)
	}

	val grass3: TextureSet by lazy {
		TextureSet(top = atlas.coordsOf(Textures.grassTop3),
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

	val water: TextureSet by lazy {
		val waterTxr = atlas.coordsOf(Textures.water)
		TextureSet(top = waterTxr,
			bottom = waterTxr,
			left = waterTxr,
			right = waterTxr,
			front = waterTxr,
			back = waterTxr
		)
	}
}

class Block(val blockType: Int) {
	companion object {
		const val air: Int = 0
		const val grass: Int = 1
		const val dirt: Int = 2
		const val snow: Int = 3
		const val water: Int = 4

		private val textureSets: Array<TextureSet?> get() = arrayOf(
			null,
			listOf(TextureSets.grass1, TextureSets.grass2, TextureSets.grass3).pickRandom(),
			TextureSets.dirt,
			TextureSets.snow,
			TextureSets.water
		)
	}

	val textureSet: TextureSet? by lazy {
		if (blockType <= 4)
			textureSets[blockType]
		else
			null
	}

	val isFilled: Boolean get() = blockType != air
}
