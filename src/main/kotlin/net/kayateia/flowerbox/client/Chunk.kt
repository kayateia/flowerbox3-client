/*
	Flowerbox
	Copyright 2018 Kayateia

	This code is licensed under the MIT license; please see LICENSE.md in the root of the project.
 */

package net.kayateia.flowerbox.client

import com.flowpowered.noise.module.source.Perlin
import net.kayateia.flowerbox.common.collections.FastFloatBuffer
import net.kayateia.flowerbox.common.collections.FastShortBuffer
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
				if (heightMinMaxed == 1)
					cubeMap[coord(x, 0, z)] = Block(Block.water)
				else {
					for (y in 0 until heightMinMaxed)
						cubeMap[coord(x, y, z)] = Block(Block.dirt)
					cubeMap[coord(x, heightMinMaxed, z)] = if (heightMinMaxed < 12) Block(Block.grass) else Block(Block.snow)
				}
			}
		}

		return cubeMap
	}

	class EdgeNeighbors(val ft: Boolean = false, val fl: Boolean = false, val fb: Boolean = false, val fr: Boolean = false,
			val tl: Boolean = false, val tr: Boolean = false, val botl: Boolean = false, val botr: Boolean = false,
			val backt: Boolean = false, val backl: Boolean = false, val backb: Boolean = false, val backr: Boolean = false) {

		companion object {
			private val trTable: Array<TR> = arrayOf(
				TR(3, 0),		// 0000
				TR(4, 270),	// 0001
				TR(4, 0),		// 0010
				TR(0, 0),		// 0011
				TR(4, 90),	// 0100
				TR(2, 180),	// 0101
				TR(0, 90),	// 0110
				TR(5, 0),		// 0111
				TR(4, 180),	// 1000
				TR(0, 270),	// 1001
				TR(2, 0),		// 1010
				TR(5, 270),	// 1011
				TR(0, 180),	// 1100
				TR(5, 180),	// 1101
				TR(5, 90),	// 1110
				TR(1, 0)		// 1111
			)
		}

		private fun coordType(type: Int) = Texture2c(
			0.25f * (type % 4), 0.25f * (type / 4),
			0.25f + 0.25f * (type % 4), 0.25f + 0.25f * (type / 4)
		)

		private fun rotate(deg: Int, coord: TextureQuad): TextureQuad = when (deg) {
			0 -> coord
			90 -> Texture4c(ulIn = coord.ll, llIn = coord.lr, lrIn = coord.ur, urIn = coord.ul)
			180 -> Texture4c(ulIn = coord.lr, llIn = coord.ur, lrIn = coord.ul, urIn = coord.ll)
			270 -> Texture4c(ulIn = coord.ur, llIn = coord.ul, lrIn = coord.ll, urIn = coord.lr)
			else -> throw Exception("fooz")
		}

		private fun edgesToMask(edges: Array<Boolean>) =
			((if (edges[0]) 1 else 0) shl 3) or
			((if (edges[1]) 1 else 0) shl 2) or
			((if (edges[2]) 1 else 0) shl 1) or
			((if (edges[3]) 1 else 0) shl 0)

		data class TR(val type: Int, val rot: Int)

		private fun edgesToTypeAndRot(edges: Array<Boolean>): TR {
			val mask = edgesToMask(edges)
			return trTable[mask]
		}

		private fun calc(edges: Array<Boolean>): TextureQuad {
			val tr = edgesToTypeAndRot(edges)
			return rotate(tr.rot, coordType(tr.type))
		}

		val frontAo: TextureQuad by lazy { calc(arrayOf(ft, fl, fb, fr)) }
		val leftAo: TextureQuad by lazy { calc(arrayOf(tl, backl, botl, fl)) }
		val rightAo: TextureQuad by lazy { calc(arrayOf(tr, fr, botr, backr)) }
		val topAo: TextureQuad by lazy { calc(arrayOf(backt, tl, ft, tr)) }
		val botAo: TextureQuad by lazy { calc(arrayOf(fb, botl, backb, botr)) }
		val backAo: TextureQuad by lazy { calc(arrayOf(backt, backr, backb, backl)) }
	}

	// Represents one voxel derived from the cube map.
	class Voxel(val x: Int, val y: Int, val z: Int, private val blockType: Block,
				private val top: Boolean = true, private val left: Boolean = true,
				private val right: Boolean = true, private val bottom: Boolean = true,
				private val front: Boolean = true, private val back: Boolean = true) {

		companion object {
			// Voxel size
			private const val size = 0.52f
			const val vertStride: Int = 10
			// const val vertStride: Int = 12
		}

		private var edges: EdgeNeighbors = EdgeNeighbors()

		fun setEdges(e: EdgeNeighbors): Voxel {
			edges = e
			return this
		}

		val isEmpty get() = !top && !left && !right && !bottom && !front && !back

		// Each vertex will take 8 floats in the array.
		fun toBuffer(dataArray: FastFloatBuffer, elementArray: FastShortBuffer) {
			/* val lm = Texture2c(0f, 0f, 0.25f, 0.25f)
			val rm = Texture2c(0.25f, 0f, 0.5f, 0.25f)
			val tm = Texture2c(0.5f, 0f, 0.75f, 0.25f)
			val btm = Texture2c(0.75f, 0f, 1f, 0.25f)
			val bkm = Texture2c(0f, 0.25f, 0.25f, 0.5f)
			var fm = Texture2c(0.25f, 0.25f, 0.5f, 0.5f) */
			if (top) {
				val txr = blockType.textureSet?.top ?: Texture2c(0f, 0f, 0f, 0f)
				val base = dataArray.length / vertStride
				val ao = edges.topAo
				elementArray.addAll(
					base+0, base+1, base+2, base+2, base+3, base+0
				)
				dataArray.addAll(
					x-size, y+size, z-size,
					0f, 1f, 0f,
					txr.ul.s, txr.ul.t,
					ao.ul.s, ao.ul.t,
					//tm.ul.s, tm.ul.t,
					x-size, y+size, z+size,
					0f, 1f, 0f,
					txr.ll.s, txr.ll.t,
					ao.ll.s, ao.ll.t,
					//tm.ll.s, tm.ll.t,
					x+size, y+size, z+size,
					0f, 1f, 0f,
					txr.lr.s, txr.lr.t,
					ao.lr.s, ao.lr.t,
					//tm.lr.s, tm.lr.t,
					x+size, y+size, z-size,
					0f, 1f, 0f,
					txr.ur.s, txr.ur.t,
					ao.ur.s, ao.ur.t//,
					//tm.ur.s, tm.ur.t
				)
			}
			if (bottom) {
				val txr = blockType.textureSet?.bottom ?: Texture2c(0f, 0f, 0f, 0f)
				val base = dataArray.length / vertStride
				val ao = edges.botAo
				elementArray.addAll(
					base+0, base+1, base+2, base+2, base+3, base+0
				)
				dataArray.addAll(
					x-size, y-size, z+size,
					0f, -1f, 0f,
					txr.ul.s, txr.ul.t,
					ao.ul.s, ao.ul.t,
					//btm.ul.s, btm.ul.t,
					x-size, y-size, z-size,
					0f, -1f, 0f,
					txr.ll.s, txr.ll.t,
					ao.ll.s, ao.ll.t,
					//btm.ll.s, btm.ll.t,
					x+size, y-size, z-size,
					0f, -1f, 0f,
					txr.lr.s, txr.lr.t,
					ao.lr.s, ao.lr.t,
					//btm.lr.s, btm.lr.t,
					x+size, y-size, z+size,
					0f, -1f, 0f,
					txr.ur.s, txr.ur.t,
					ao.ur.s, ao.ur.t//,
					//btm.ur.s, btm.ur.t
				)
			}
			if (front) {
				val txr = blockType.textureSet?.front ?: Texture2c(0f, 0f, 0f, 0f)
				val base = dataArray.length / vertStride
				val ao = edges.frontAo
				elementArray.addAll(
					base+0, base+1, base+2, base+2, base+3, base+0
				)
				dataArray.addAll(
					x-size, y+size, z+size,
					0f, 0f, 1f,
					txr.ul.s, txr.ul.t,
					ao.ul.s, ao.ul.t,
					// fm.ul.s, fm.ul.t,
					x-size, y-size, z+size,
					0f, 0f, 1f,
					txr.ll.s, txr.ll.t,
					ao.ll.s, ao.ll.t,
					//fm.ll.s, fm.ll.t,
					x+size, y-size, z+size,
					0f, 0f, 1f,
					txr.lr.s, txr.lr.t,
					ao.lr.s, ao.lr.t,
					//fm.lr.s, fm.lr.t,
					x+size, y+size, z+size,
					0f, 0f, 1f,
					txr.ur.s, txr.ur.t,
					ao.ur.s, ao.ur.t//,
					//fm.ur.s, fm.ur.t
				)
			}
			if (back) {
				val txr = blockType.textureSet?.back ?: Texture2c(0f, 0f, 0f, 0f)
				val base = dataArray.length / vertStride
				val ao = edges.backAo
				elementArray.addAll(
					base+0, base+1, base+2, base+2, base+3, base+0
				)
				dataArray.addAll(
					x+size, y+size, z-size,
					0f, 0f, -1f,
					txr.ul.s, txr.ul.t,
					ao.ul.s, ao.ul.t,
					//bkm.ul.s, bkm.ul.t,
					x+size, y-size, z-size,
					0f, 0f, -1f,
					txr.ll.s, txr.ll.t,
					ao.ll.s, ao.ll.t,
					//bkm.ll.s, bkm.ll.t,
					x-size, y-size, z-size,
					0f, 0f, -1f,
					txr.lr.s, txr.lr.t,
					ao.lr.s, ao.lr.t,
					//bkm.lr.s, bkm.lr.t,
					x-size, y+size, z-size,
					0f, 0f, -1f,
					txr.ur.s, txr.ur.t,
					ao.ur.s, ao.ur.t//,
					//bkm.ur.s, bkm.ur.t
				)
			}
			if (left) {
				val txr = blockType.textureSet?.left ?: Texture2c(0f, 0f, 0f, 0f)
				val base = dataArray.length / vertStride
				val ao = edges.leftAo
				elementArray.addAll(
					base+0, base+1, base+2, base+2, base+3, base+0
				)
				dataArray.addAll(
					x-size, y+size, z-size,
					-1f, 0f, 0f,
					txr.ul.s, txr.ul.t,
					ao.ul.s, ao.ul.t,
					//lm.ul.s, lm.ul.t,
					x-size, y-size, z-size,
					-1f, 0f, 0f,
					txr.ll.s, txr.ll.t,
					ao.ll.s, ao.ll.t,
					//lm.ll.s, lm.ll.t,
					x-size, y-size, z+size,
					-1f, 0f, 0f,
					txr.lr.s, txr.lr.t,
					ao.lr.s, ao.lr.t,
					//lm.lr.s, lm.lr.t,
					x-size, y+size, z+size,
					-1f, 0f, 0f,
					txr.ur.s, txr.ur.t,
					ao.ur.s, ao.ur.t//,
					//lm.ur.s, lm.ur.t
				)
			}
			if (right) {
				val txr = blockType.textureSet?.right ?: Texture2c(0f, 0f, 0f, 0f)
				val base = dataArray.length / vertStride
				val ao = edges.rightAo
				elementArray.addAll(
					base+0, base+1, base+2, base+2, base+3, base+0
				)
				dataArray.addAll(
					x+size, y+size, z+size,
					1f, 0f, 0f,
					txr.ul.s, txr.ul.t,
					ao.ul.s, ao.ul.t,
					//rm.ul.s, rm.ul.t,
					x+size, y-size, z+size,
					1f, 0f, 0f,
					txr.ll.s, txr.ll.t,
					ao.ll.s, ao.ll.t,
					//rm.ll.s, rm.ll.t,
					x+size, y-size, z-size,
					1f, 0f, 0f,
					txr.lr.s, txr.lr.t,
					ao.lr.s, ao.lr.t,
					//rm.lr.s, rm.lr.t,
					x+size, y+size, z-size,
					1f, 0f, 0f,
					txr.ur.s, txr.ur.t,
					ao.ur.s, ao.ur.t//,
					//rm.ur.s, rm.ur.t
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
					fun isFilledRaw(x: Int, y: Int, z: Int) = cubeMap[coord(x, y, z)].isFilled
					fun isFilled(x: Int, y: Int, z: Int): Boolean {
						if (x < 0 || x > 15)
							return false
						if (y < 0 || y > 15)
							return false
						if (z < 0 || z > 15)
							return false

						return isFilledRaw(x, y, z)
					}
					if (isFilled(x, y, z)) {
						val voxel = Voxel(x, y, z,
							cubeMap[coord(x, y, z)],
							top = !isFilled(x, y+1, z),
							left = !isFilled(x - 1, y, z),
							right = !isFilled(x + 1, y, z),
							bottom = !isFilled(x, y - 1, z),
							back = !isFilled(x, y, z - 1),
							front = !isFilled(x, y, z + 1)
						)
						if (!voxel.isEmpty) {
							val edges = EdgeNeighbors(
								ft = isFilled(x, y+1, z+1),
								fl = isFilled(x-1, y, z+1),
								fb = isFilled(x, y-1, z+1),
								fr = isFilled(x+1, y, z+1),
								tl = isFilled(x-1, y+1, z),
								tr = isFilled(x+1, y+1, z),
								botl = isFilled(x-1, y-1, z),
								botr = isFilled(x+1, y-1, z),
								backt = isFilled(x, y+1, z-1),
								backl = isFilled(x-1, y, z-1),
								backb = isFilled(x, y-1, z-1),
								backr = isFilled(x+1, y, z-1)
							)
							displayList += voxel.setEdges(edges)
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
		val vas = FastFloatBuffer(voxels.size * 6 * 4 * Voxel.vertStride)
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
		glVertexAttribPointer(0, 3, GL_FLOAT, false, Voxel.vertStride*4, 0)
		// Put the normal components in attribute list 1
		glVertexAttribPointer(1, 3, GL_FLOAT, false, Voxel.vertStride*4, 3*4)
		// Put the texture coordinates in attribute list 2
		glVertexAttribPointer(2, 2, GL_FLOAT, false, Voxel.vertStride*4, 6*4)
		// Put the AO texture coordinates in attribute list 3
		glVertexAttribPointer(3, 2, GL_FLOAT, false, Voxel.vertStride*4, 8*4)
		//glVertexAttribPointer(4, 2, GL_FLOAT, false, Voxel.vertStride*4, 10*4)

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
		glActiveTexture(GL_TEXTURE1)
		glBindTexture(GL_TEXTURE_2D, Textures.aoTxr.glTextureId)
		//glActiveTexture(GL_TEXTURE2)
		//glBindTexture(GL_TEXTURE_2D, Textures.sdTxr.glTextureId)

		// Bind to the VAO that has all the information about the vertices
		glBindVertexArray(vaoId)
		glEnableVertexAttribArray(0)
		glEnableVertexAttribArray(1)
		glEnableVertexAttribArray(2)
		glEnableVertexAttribArray(3)
		//glEnableVertexAttribArray(4)

		// Bind to the index VBO that has all the information about the order of the vertices
		glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, vboiId)

		// Draw the vertices
		glDrawElements(GL_TRIANGLES, buffers.elems.length, GL_UNSIGNED_SHORT, 0)

		// Put everything back to default (deselect)
		glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, 0)
		glDisableVertexAttribArray(0)
		glDisableVertexAttribArray(1)
		glDisableVertexAttribArray(2)
		glDisableVertexAttribArray(3)
		//glDisableVertexAttribArray(4)
		glBindVertexArray(0)

		glUseProgram(0)
	}
}

