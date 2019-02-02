/*
	Flowerbox
	Copyright 2018 Kayateia

	This code is licensed under the GNU Public License version 3 or higher.
	Please see LICENSE.txt in the root of the project for more details.
 */

package net.kayateia.flowerbox.client

import org.lwjgl.opengl.GL11.*
import slim.texture.io.PNGDecoder
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder

class SingleTexture(val filename: String) {
	val glTextureId: Int by lazy { genTextureId() }

	private fun genTextureId(): Int {
		val decoder = PNGDecoder(File(filename).toURI().toURL().openStream())
		val buffer = ByteBuffer.allocateDirect(decoder.width * decoder.height * 4)
		buffer.order(ByteOrder.nativeOrder())
		decoder.decode(buffer, decoder.width * 4, PNGDecoder.Format.RGBA)
		buffer.flip()

		val txrId = glGenTextures()
		glBindTexture(GL_TEXTURE_2D, txrId)
		glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR)
		glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR)
		glTexImage2D(GL_TEXTURE_2D, 0, GL_RGB, decoder.width, decoder.height, 0, GL_RGBA, GL_UNSIGNED_BYTE, buffer)

		return txrId
	}
}
