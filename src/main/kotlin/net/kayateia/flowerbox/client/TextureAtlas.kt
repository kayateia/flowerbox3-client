/*
	Flowerbox
	Copyright 2018 Kayateia

	Please see LICENSE.txt in the root of the project for more details.
 */

package net.kayateia.flowerbox.client

import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder

import org.lwjgl.opengl.GL11.*
import slim.texture.io.PNGDecoder


class TextureCoord(val s: Float, val t: Float)

interface TextureQuad {
	val ul: TextureCoord
	val ur: TextureCoord
	val ll: TextureCoord
	val lr: TextureCoord
}

class Texture2c(val uls: Float, val ult: Float, val lrs: Float, val lrt: Float) : TextureQuad {
	override val ul: TextureCoord
		get() = TextureCoord(uls, ult)
	override val ur: TextureCoord
		get() = TextureCoord(lrs, ult)
	override val ll: TextureCoord
		get() = TextureCoord(uls, lrt)
	override val lr: TextureCoord
		get() = TextureCoord(lrs, lrt)
}

class Texture4c(val ulIn: TextureCoord, val llIn: TextureCoord, val lrIn: TextureCoord, val urIn: TextureCoord) : TextureQuad {
	override val ul: TextureCoord
		get() = ulIn
	override val ll: TextureCoord
		get() = llIn
	override val lr: TextureCoord
		get() = lrIn
	override val ur: TextureCoord
		get() = urIn
}

class TextureAtlas(val eachTxrSize: Int) {
	private val queuedImages = ArrayList<String>()
	private var glTxrId = 0
	private var bigTxrSize = 0
	private var coords: Array<TextureQuad> = arrayOf()

	fun load(filenames: List<String>) {
		queuedImages.addAll(filenames)
	}

	val glTextureId: Int
		get() = if (glTxrId != 0) glTxrId else genTextureId()

	fun coordsOf(index: Int): TextureQuad = coords[index]

	private fun genTextureId(): Int {
		bigTxrSize = txrSize
		val txrPerRow: Int = bigTxrSize / eachTxrSize
		val buffer = ByteBuffer.allocateDirect(bigTxrSize * bigTxrSize * 4)
		buffer.order(ByteOrder.nativeOrder())

		val poses = sequence {
			for (y in 0 until txrPerRow) {
				for (x in 0 until txrPerRow) {
					yield((x * eachTxrSize * 4) + (y * eachTxrSize * bigTxrSize * 4))
				}
			}
		}.iterator()
		coords = sequence {
			for (y in 0 until txrPerRow) {
				for (x in 0 until txrPerRow) {
					yield(Texture2c(
						(1f * x * eachTxrSize) / bigTxrSize,
						(1f * y * eachTxrSize) / bigTxrSize,
						((1f * x * eachTxrSize) + (eachTxrSize-1)) / bigTxrSize,
						((1f * y * eachTxrSize) + (eachTxrSize-1)) / bigTxrSize))
				}
			}
		}.toList().toTypedArray()

		val stride = bigTxrSize * 4
		for (fn in queuedImages) {
			val offset = poses.next()
			println("$fn at $offset")
			loadPng(fn, buffer, offset, stride)
		}

		buffer.flip()

		glTxrId = glGenTextures()
		glBindTexture(GL_TEXTURE_2D, glTxrId)
		glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR)
		glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR)
		//glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE)
		//glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE)

		glTexImage2D(GL_TEXTURE_2D, 0, GL_RGB, bigTxrSize, bigTxrSize, 0, GL_RGBA, GL_UNSIGNED_BYTE, buffer)

		return glTxrId
	}

	private val txrSize: Int get() {
		val minTxrMultiple = Math.ceil(Math.sqrt(queuedImages.size.toDouble()))
		val minTxrSize = minTxrMultiple * eachTxrSize

		val p2s = (0 to 12).toList().map { Math.pow(2.0, it.toDouble()) }.dropWhile { it < minTxrSize }.take(1)
		if (p2s.isEmpty())
			throw Exception("fooz")

		return p2s.first().toInt()
	}

	private fun loadPng(path: String, buffer: ByteBuffer, offset: Int, stride: Int) {
		println("Loading $path")
		val decoder = PNGDecoder(File(path).toURI().toURL().openStream())
		/* val width = decoder.width
		val height = decoder.height */

		buffer.position(offset)
		decoder.decode(buffer, stride, PNGDecoder.Format.RGBA)
	}
}
