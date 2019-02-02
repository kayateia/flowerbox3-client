/*
	Flowerbox
	Copyright 2018 Kayateia

	This code is licensed under the GNU Public License version 3 or higher.
	Please see LICENSE.txt in the root of the project for more details.
 */

package net.kayateia.flowerbox.client

import net.kayateia.flowerbox.common.cherry.parser.Parser
import net.kayateia.flowerbox.common.cherry.runtime.*
import net.kayateia.flowerbox.common.world.cherry.NativeVector
import net.kayateia.flowerbox.common.world.cherry.WorldCherryLibrary
import org.lwjgl.BufferUtils
import org.lwjgl.opengl.GL11.*
import org.lwjgl.opengl.GL20.*
import org.lwjgl.util.vector.*
import java.util.*
import kotlin.concurrent.thread

typealias CherryRuntime = net.kayateia.flowerbox.common.cherry.runtime.Runtime

object Renderer {
	// How many chunks we'll render in our small world.
	private const val chunkW = 15
	private const val chunkH = 15
	private val chunks = Array<Chunk?>(chunkW * chunkH) { null }

	// The width/height of the viewport.
	private var width: Int = 0
	private var height: Int = 0

	// Queue of chunks that remain to be turned into VAO/VBOs.
	private val chunkRenderQueue: Queue<Chunk> = ArrayDeque<Chunk>()

	private val lightingProgram = """
		namespace fb.test;

		class LightSource {
			private i = 0;
			private pos;

			public init(pos) {
				self.pos = pos;
				self.pos.x = 50.0;
				self.pos.y = 30.0;
				self.pos.z = 0.0;
			}

			public perFrame() {
				self.i = (self.i + 1) % 360;

				var rads = self.i * 3.1415 / 180.0;
				self.updatePos(rads);
			}

			private updatePos(rads) {
				self.pos.x = 50.0 * sys.math.cos(rads);
				self.pos.z = 50.0 * sys.math.sin(rads);
			}
		}
	"""

	private val parsedProgram = Parser().parse("<inline>", lightingProgram)
	private var cherry: CherryRuntime = CherryRuntime()
	private var lightSource: ObjectValue? = null
	private var lightPos: Value? = null
	private val lightPosNative = NativeVector()

	init {
		WorldCherryLibrary.registerAll(cherry)
		cherry.execute(parsedProgram)
		lightPos = cherry.wrapNative(lightPosNative, NativeVector.Companion)
		cherry.executeNew("fb.test.LightSource", listOf(lightPos!!))
		lightSource = cherry.result as ObjectValue
	}

	private fun lightingProgramPerFrame() {
		cherry.executeMethod(lightSource!!, "perFrame", listOf())
		glUniform3f(lightPositionLocation,
			lightPosNative.x.toFloat(),
			lightPosNative.y.toFloat(),
			lightPosNative.z.toFloat()
		)
	}

	// Called once, in the render loop.
	fun setup(w: Int, h: Int) {
		width = w
		height = h

		// Pull this once during the setup to make sure it gets loaded.
		println("Atlas ID is ${Textures.atlas.glTextureId}")
		println("AO txr ID is ${Textures.aoTxr.glTextureId}")
		// println("SD txr ID is ${Textures.sdTxr.glTextureId}")

		setupMatrices()
		setupShaders()

		// Begin background terrain generation.
		thread(start = true) {
			println("Beginning terrain generation")
			for (x in 0 until chunkW) {
				for (z in 0 until chunkH) {
					val newChunk = Chunk(x + Chunk.xSize, z + Chunk.zSize)
					newChunk.setup()
					newChunk.buffers
					println("done with $x, $z")
					synchronized(chunkRenderQueue) {
						chunkRenderQueue.add(newChunk)
					}
					chunks[x * chunkW + z] = newChunk
				}
			}
			println("terrain generation done")
		}
	}

	private const val PI: Double = 3.14159265358979323846
	private fun coTangent(angle: Float) = (1.0 / Math.tan(angle.toDouble())).toFloat()
	private fun degreesToRadians(degrees: Float) = (degrees * (PI / 180.0)).toFloat()

	private val projectionMatrix = Matrix4f()
	private val viewMatrix = Matrix4f()
	private val modelMatrix = Matrix4f()
	private val matrix44buffer = BufferUtils.createFloatBuffer(16)

	private fun setupMatrices() {
		val fieldOfView = 60f
		val aspectRatio = width.toFloat() / height.toFloat()
		val near_plane = 0.1f
		val far_plane = 200f

		val y_scale = this.coTangent(this.degreesToRadians(fieldOfView / 2f))
		val x_scale = y_scale / aspectRatio
		val frustum_length = far_plane - near_plane

		projectionMatrix.m00 = x_scale
		projectionMatrix.m11 = y_scale
		projectionMatrix.m22 = -((far_plane + near_plane) / frustum_length)
		projectionMatrix.m23 = -1f
		projectionMatrix.m32 = -((2 * near_plane * far_plane) / frustum_length)
		projectionMatrix.m33 = 0f
	}

	private var pId: Int = 0
	private var projectionMatrixLocation: Int = 0
	private var viewMatrixLocation: Int = 0
	private var modelMatrixLocation: Int = 0
	private var lightColorLocation: Int = 0
	private var lightPositionLocation: Int = 0
	private var ambientLocation: Int = 0
	private var diffuseTxrLocation: Int = 0
	private var aoTxrLocation: Int = 0
	// private var sdTxrLocation: Int = 0

	private fun setupShaders() {
		// Create a new shader program that links both shaders
		pId = glCreateProgram()

		// Load the vertex shader
		val vsId = Shader("shaders/basic_vertex.glsl", GL_VERTEX_SHADER)
		// Load the fragment shader
		val fsId = Shader("shaders/basic_fragment.glsl", GL_FRAGMENT_SHADER)

		glAttachShader(pId, vsId.shaderId)
		glAttachShader(pId, fsId.shaderId)

		// Position information will be attribute 0
		glBindAttribLocation(pId, 0, "in_Position")
		// Normal information will be attribute 1
		glBindAttribLocation(pId, 1, "in_Normal")
		// Texture information will be attribute 2
		glBindAttribLocation(pId, 2, "in_TextureCoord")
		// AO Texture information will be attribute 3
		glBindAttribLocation(pId, 3, "in_AoTextureCoord")
		// glBindAttribLocation(pId, 4, "in_sdTextureCoord")

		glLinkProgram(pId)
		val result = IntArray(1)
		glGetProgramiv(pId, GL_LINK_STATUS, result)
		if (result[0] == 0) {
			val logs = glGetProgramInfoLog(pId)
			println(logs)
		}
		glValidateProgram(pId)

		// Get matrices uniform locations
		projectionMatrixLocation = glGetUniformLocation(pId, "projectionMatrix")
		viewMatrixLocation = glGetUniformLocation(pId, "viewMatrix")
		modelMatrixLocation = glGetUniformLocation(pId, "modelMatrix")

		// Get texture locations.
		diffuseTxrLocation = glGetUniformLocation(pId, "diffuseTexture")
		aoTxrLocation = glGetUniformLocation(pId, "aoTexture")
		// sdTxrLocation = glGetUniformLocation(pId, "sdTexture")

		// Get other parameter locations.
		lightColorLocation = glGetUniformLocation(pId, "lightColor")
		lightPositionLocation = glGetUniformLocation(pId, "lightPos")
		ambientLocation = glGetUniformLocation(pId, "ambientStrength")

		glUseProgram(pId)

		// Set some default lighting.
		glUniform3f(lightColorLocation, 0.5f, 0.5f, 0.5f)
		glUniform3f(lightPositionLocation, 15f, 30f, 0f)
		glUniform1f(ambientLocation, 0.7f)

		// And textures.
		glUniform1i(diffuseTxrLocation, 0)
		glUniform1i(aoTxrLocation, 1)
		// glUniform1i(sdTxrLocation, 2)

		glUseProgram(0)

		// this.exitOnGLError("setupShaders");
	}

	private fun loadModel() {
		modelMatrix.store(matrix44buffer)
		matrix44buffer.flip()
		glUniformMatrix4fv(modelMatrixLocation, false, matrix44buffer)
	}

	// Camera rotation and position+delta position.
	var xrot = 0.0f
	var yrot = 0.0f
	var pos = Vector3f(16.0f/2.0f, 10f, 16.0f/2.0f)
	var moveVector = Vector3f(0f, 0f, 0f)

	private fun move() {
		val yr = degreesToRadians(yrot).toDouble()
		val xr = degreesToRadians(xrot).toDouble()
		pos.x += Math.sin(yr).toFloat() * moveVector.z - Math.cos(yr).toFloat() * moveVector.x
		pos.y -= Math.sin(xr).toFloat() * moveVector.z / 2f + moveVector.y
		pos.z -= Math.cos(yr).toFloat() * moveVector.z + Math.sin(yr).toFloat() * moveVector.x
	}

	fun render() {
		glViewport(0, 0, width, height)
		glClearColor(183f / 255f, 235f / 255f, 1f, 0f)
		glClear(GL_COLOR_BUFFER_BIT or GL_DEPTH_BUFFER_BIT)

		glEnable(GL_DEPTH_TEST)
		glDepthFunc(GL_LESS)
		glEnable(GL_CULL_FACE)
		glHint(GL_PERSPECTIVE_CORRECTION_HINT, GL_NICEST)
		//glEnable(GL_BLEND)
		//glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA)

		move()

		viewMatrix.setIdentity()
		viewMatrix.rotate(degreesToRadians(xrot), Vector3f(1f, 0f, 0f))
		viewMatrix.rotate(degreesToRadians(yrot), Vector3f(0f, 1f, 0f))
		viewMatrix.translate(Vector3f(0f, -10f, -50f))
		viewMatrix.translate(pos)
		/*viewMatrix.rotate(degreesToRadians(20f), Vector3f(1f, 0f, 0f))
		viewMatrix.translate(Vector3f(0f, -10f, -50f))
		viewMatrix.rotate(degreesToRadians(yrot), Vector3f(0f, 1f, 0f))
		viewMatrix.rotate(degreesToRadians(xrot), Vector3f(1f, 0f, 0f))
		viewMatrix.translate(Vector3f(-16f/2, 0f, -16f/2)) */

		glEnable(GL_TEXTURE_2D)
		glTexEnvi(GL_TEXTURE_ENV, GL_TEXTURE_ENV_MODE, GL_MODULATE)

		glUseProgram(pId)

		projectionMatrix.store(matrix44buffer)
		matrix44buffer.flip()
		glUniformMatrix4fv(projectionMatrixLocation, false, matrix44buffer)
		viewMatrix.store(matrix44buffer)
		matrix44buffer.flip()
		glUniformMatrix4fv(viewMatrixLocation, false, matrix44buffer)
		loadModel()

		lightingProgramPerFrame()

		glUseProgram(0)

		// rot = (rot + drot) % 360

		var chunkToRender: Chunk? = null
		synchronized(chunkRenderQueue) {
			if (!chunkRenderQueue.isEmpty()) {
				chunkToRender = chunkRenderQueue.remove()
			}
		}
		chunkToRender?.createVertexArrays()

		for (x in 0 until chunkW) {
			for (z in 0 until chunkH) {
				val chunk = chunks[x * chunkW + z]
				if (chunk != null && chunk.vaoId > 0) {
					glUseProgram(pId)
					modelMatrix.setIdentity()
					modelMatrix.translate(Vector3f(-16f*chunkW/2 + 16f*x, 0f, -16f*chunkH/2 + 16f*z));
					loadModel()
					glUseProgram(0)
					chunks[x*chunkW + z]?.render(pId)
				}
			}
		}
	}
}
