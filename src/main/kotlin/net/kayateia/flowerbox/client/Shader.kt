/*
	Flowerbox
	Copyright 2018 Kayateia

	This code is licensed under the GNU Public License version 3 or higher.
	Please see LICENSE.txt in the root of the project for more details.
 */

package net.kayateia.flowerbox.client

import org.lwjgl.opengl.GL20.*
import java.io.File

class Shader(filename: String, shaderType: Int) {
	val shaderId: Int

	init {
		val lines = File(filename).readLines()
		val shaderSource = lines.fold("", { total, next -> total + next + "\n" })
		shaderId = glCreateShader(shaderType)
		glShaderSource(shaderId, shaderSource)
		glCompileShader(shaderId)

		val result = IntArray(1)
		glGetProgramiv(shaderId, GL_COMPILE_STATUS, result)
		if (result[0] == 0) {
			val logs = glGetShaderInfoLog(shaderId)
			println(logs)
		}
	}
}
