/*
	Flowerbox
	Copyright 2018 Kayateia

	This code is licensed under the MIT license; please see LICENSE.md in the root of the project.
 */

package net.kayateia.flowerbox.client

import com.flowpowered.noise.module.source.Perlin
import net.kayateia.flowerbox.client.collections.FastFloatBuffer
import net.kayateia.flowerbox.client.collections.FastShortBuffer
import org.lwjgl.BufferUtils
import org.lwjgl.opengl.GL15.*
import org.lwjgl.opengl.GL30.*
import org.lwjgl.opengl.GL11.*
import org.lwjgl.opengl.GL13.*
import org.lwjgl.opengl.GL20.*

class Chunk(private val globalX: Float, private val globalZ: Float) {
	companion object {
		// The size of one chunk in Perlin coordinates.
		const val xSize = 0.75f
		const val zSize = 0.75f

		// These are other Perlin noise knobs you can tweak for terrain variation.
		private const val octaveCount = 4
		private const val persistence = 0.3

		// Convert from x,y,z to cubeMap index.
		private fun coord(x: Int, y: Int, z: Int) = z*16*16 + y*16 + x
	}

	fun setup() {
		genLand()
	}

	private val land by lazy { genLand() }
	private val displayList by lazy { genDL(land) }
	val buffers by lazy { genBuffers(displayList) }

	// Generates the cube map.
	private fun genLand(): Array<Block> {
		val cubeMap = Array(16*16*16) { Block(Block.air) }

		val xMin = globalX * Chunk.xSize
		val zMin = globalZ * Chunk.zSize
		val perlin = Perlin()
		perlin.octaveCount = Chunk.octaveCount
		perlin.persistence = Chunk.persistence

		for (x in 0 until 16) {
			for (z in 0 until 16) {
				// val height = Math.floor(Math.random() * 15).asInstanceOf[Int]
				val perlinX = xMin + Chunk.xSize*(x / 16f)
				val perlinZ = zMin + Chunk.zSize*(z / 16f)
				val height = Math.floor(15f * perlin.getValue(perlinX.toDouble(), 0.0, perlinZ.toDouble())).toInt()
				val heightMinMaxed = Math.max(Math.min(height, 15), 1)
				for (y in 0 until heightMinMaxed)
					cubeMap[coord(x, y, z)] = Block(Block.dirt)
				cubeMap[coord(x, heightMinMaxed, z)] = if (heightMinMaxed < 12) Block(Block.grass) else Block(Block.snow)
			}
		}

		return cubeMap
	}

	// Represents one voxel derived from the cube map.
	class Voxel(val x: Int, val y: Int, val z: Int, private val blockType: Block,
				private val top: Boolean = true, private val left: Boolean = true,
				private val right: Boolean = true, private val bottom: Boolean = true,
				private val front: Boolean = true, private val back: Boolean = true) {

		companion object {
			// Voxel size
			private const val size = 0.52f
		}

		val isEmpty get() = !top && !left && !right && !bottom && !front && !back

		// Each vertex will take 8 floats in the array.
		fun toBuffer(dataArray: FastFloatBuffer, elementArray: FastShortBuffer) {
			if (top) {
				val txr = blockType.textureSet?.top ?: TextureCoord(0f, 0f, 0f, 0f)
				val base = dataArray.length / 8
				elementArray.addAll(
					base+0, base+1, base+2, base+2, base+3, base+0
				)
				dataArray.addAll(
					x+size, y+size, z-size,
					0f, 1f, 0f,
					txr.s2, txr.t1,
					x-size, y+size, z-size,
					0f, 1f, 0f,
					txr.s1, txr.t1,
					x-size, y+size, z+size,
					0f, 1f, 0f,
					txr.s1, txr.t2,
					x+size, y+size, z+size,
					0f, 1f, 0f,
					txr.s2, txr.t2
				)
			}
			if (bottom) {
				val txr = blockType.textureSet?.bottom ?: TextureCoord(0f, 0f, 0f, 0f)
				val base = dataArray.length / 8
				elementArray.addAll(
						base+0, base+1, base+2, base+2, base+3, base+0
				)
				dataArray.addAll(
					x+size, y-size, z+size,
					0f, -1f, 0f,
					txr.s2, txr.t1,
					x-size, y-size, z+size,
					0f, -1f, 0f,
					txr.s1, txr.t1,
					x-size, y-size, z-size,
					0f, -1f, 0f,
					txr.s1, txr.t2,
					x+size, y-size, z-size,
					0f, -1f, 0f,
					txr.s2, txr.t2
				)
			}
			if (back) {
				val txr = blockType.textureSet?.back ?: TextureCoord(0f, 0f, 0f, 0f)
				val base = dataArray.length / 8
				elementArray.addAll(
						base+0, base+1, base+2, base+2, base+3, base+0
				)
				dataArray.addAll(
					x+size, y+size, z+size,
					0f, 0f, 1f,
					txr.s2, txr.t1,
					x-size, y+size, z+size,
					0f, 0f, 1f,
					txr.s1, txr.t1,
					x-size, y-size, z+size,
					0f, 0f, 1f,
					txr.s1, txr.t2,
					x+size, y-size, z+size,
					0f, 0f, 1f,
					txr.s2, txr.t2
				)
			}
			if (front) {
				val txr = blockType.textureSet?.front ?: TextureCoord(0f, 0f, 0f, 0f)
				val base = dataArray.length / 8
				elementArray.addAll(
						base+0, base+1, base+2, base+2, base+3, base+0
				)
				dataArray.addAll(
					x+size, y-size, z-size,
					0f, 0f, -1f,
					txr.s2, txr.t2,
					x-size, y-size, z-size,
					0f, 0f, -1f,
					txr.s1, txr.t2,
					x-size, y+size, z-size,
					0f, 0f, -1f,
					txr.s1, txr.t1,
					x+size, y+size, z-size,
					0f, 0f, -1f,
					txr.s2, txr.t1
				)
			}
			if (left) {
				val txr = blockType.textureSet?.left ?: TextureCoord(0f, 0f, 0f, 0f)
				val base = dataArray.length / 8
				elementArray.addAll(
						base+0, base+1, base+2, base+2, base+3, base+0
				)
				dataArray.addAll(
					x-size, y+size, z+size,
					-1f, 0f, 0f,
					txr.s2, txr.t1,
					x-size, y+size, z-size,
					-1f, 0f, 0f,
					txr.s1, txr.t1,
					x-size, y-size, z-size,
					-1f, 0f, 0f,
					txr.s1, txr.t2,
					x-size, y-size, z+size,
					-1f, 0f, 0f,
					txr.s2, txr.t2
				)
			}
			if (right) {
				val txr = blockType.textureSet?.right ?: TextureCoord(0f, 0f, 0f, 0f)
				val base = dataArray.length / 8
				elementArray.addAll(
						base+0, base+1, base+2, base+2, base+3, base+0
				)
				dataArray.addAll(
					x+size, y+size, z-size,
					1f, 0f, 0f,
					txr.s2, txr.t1,
					x+size, y+size, z+size,
					1f, 0f, 0f,
					txr.s1, txr.t1,
					x+size, y-size, z+size,
					1f, 0f, 0f,
					txr.s1, txr.t2,
					x+size, y-size, z-size,
					1f, 0f, 0f,
					txr.s2, txr.t2
				)
			}
		}
	}

	// Converts the cube map into a display list of voxels. Optimizes away as many as possible.
	private fun genDL(cubeMap: Array<Block>): MutableList<Voxel> {
		val displayList = ArrayList<Voxel>(16*16*16)

		for (x in 0 until 16) {
			for (y in 0 until 16) {
				for (z in 0 until 16) {
					fun isFilled(x: Int, y: Int, z: Int) = cubeMap[coord(x, y, z)].isFilled
					if (isFilled(x, y, z)) {
						fun onEdge(c: Int) = c == 0 || c == 15
						fun isEdgeBlock(x: Int, y: Int, z: Int) = onEdge(x) || onEdge(y) || onEdge(z)
						if (isEdgeBlock(x, y, z)) {
							displayList += Voxel(x, y, z, cubeMap[coord(x, y, z)])
						} else {
							val voxel = Voxel(x, y, z,
								cubeMap[coord(x, y, z)],
								!isFilled(x, y + 1, z),
								!isFilled(x - 1, y, z),
								!isFilled(x + 1, y, z),
								!isFilled(x, y - 1, z),
								!isFilled(x, y, z - 1),
								!isFilled(x, y, z + 1)
							)
							if (!voxel.isEmpty)
								displayList += voxel
						}
					}
				}
			}
		}

		return displayList
	}

	data class Buffers(val vertsAndSt: FastFloatBuffer, val elems: FastShortBuffer)

	private fun genBuffers(voxels: MutableList<Voxel>): Buffers {
		// These initial capacities are a worst case estimate, so they will basically always overrun reality.
		val vas = FastFloatBuffer(voxels.size * 6 * 4 * 8)
		val es = FastShortBuffer(voxels.size * 6 * 6)
		for (v in voxels) {
			v.toBuffer(vas, es)
		}

		return Buffers(vas, es)
	}

	var vaoId: Int = 0
	private var vboId: Int = 0
	private var vboiId: Int = 0

	fun createVertexArrays() {
		val vertByteBuffer = BufferUtils.createByteBuffer(buffers.vertsAndSt.length * 4);
		val vertFloatBuffer = vertByteBuffer.asFloatBuffer()
		vertFloatBuffer.put(buffers.vertsAndSt.result(), 0, buffers.vertsAndSt.length)
		vertFloatBuffer.flip()

		val indexByteBuffer = BufferUtils.createByteBuffer(buffers.elems.length * 2)
		val indexShortBuffer = indexByteBuffer.asShortBuffer()
		indexShortBuffer.put(buffers.elems.result(), 0, buffers.elems.length)
		indexShortBuffer.flip()

		vaoId = glGenVertexArrays()
		glBindVertexArray(vaoId)

		vboId = glGenBuffers()
		glBindBuffer(GL_ARRAY_BUFFER, vboId)
		glBufferData(GL_ARRAY_BUFFER, vertFloatBuffer, GL_STREAM_DRAW)

		// Put the position coordinates in attribute list 0
		glVertexAttribPointer(0, 3, GL_FLOAT, false, 8*4, 0)
		// Put the normal components in attribute list 1
		glVertexAttribPointer(1, 3, GL_FLOAT, false, 8*4, 3*4)
		// Put the texture coordinates in attribute list 2
		glVertexAttribPointer(2, 2, GL_FLOAT, false, 8*4, 6*4)

		glBindBuffer(GL_ARRAY_BUFFER, 0)
		glBindVertexArray(0)

		vboiId = glGenBuffers()
		glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, vboiId)
		glBufferData(GL_ELEMENT_ARRAY_BUFFER, indexShortBuffer, GL_STATIC_DRAW)
		glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, 0)
	}

	fun render(pId: Int) {
		if (vaoId == 0)
			createVertexArrays()

		glUseProgram(pId)

		// Bind the texture
		glActiveTexture(GL_TEXTURE0)
		glBindTexture(GL_TEXTURE_2D, Textures.atlas.glTextureId)

		// Bind to the VAO that has all the information about the vertices
		glBindVertexArray(vaoId)
		glEnableVertexAttribArray(0)
		glEnableVertexAttribArray(1)
		glEnableVertexAttribArray(2)

		// Bind to the index VBO that has all the information about the order of the vertices
		glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, vboiId)

		// Draw the vertices
		glDrawElements(GL_TRIANGLES, buffers.elems.length, GL_UNSIGNED_SHORT, 0)

		// Put everything back to default (deselect)
		glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, 0)
		glDisableVertexAttribArray(0)
		glDisableVertexAttribArray(1)
		glDisableVertexAttribArray(2)
		glBindVertexArray(0)

		glUseProgram(0)
	}
}

