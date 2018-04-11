/*
	Flowerbox
	Copyright 2018 Kayateia

	This code is licensed under the MIT license; please see LICENSE.md in the root of the project.
 */

// Adapted from the scala-lwjgl project, which had this copyright:
/*******************************************************************************
 * Copyright 2015 Serf Productions, LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/

package net.kayateia.flowerbox.client

import org.lwjgl.glfw.GLFWErrorCallback
import org.lwjgl.glfw.GLFW.*
import org.lwjgl.opengl.*
import org.lwjgl.glfw.Callbacks.*
import org.lwjgl.glfw.GLFWCursorPosCallback
import org.lwjgl.glfw.GLFWKeyCallback
import org.lwjgl.glfw.GLFWMouseButtonCallback
import org.lwjgl.system.MemoryUtil.*

class RunLoop {
	companion object {
		const val WIDTH = 800
		const val HEIGHT = 600
	}

	fun run(args: Array<String>) {
		try {
			GLFWErrorCallback.createPrint(System.err).set()

			val window = init()
			loop(window)

			glfwFreeCallbacks(window)
			glfwDestroyWindow(window)
		} finally {
			glfwTerminate() // destroys all remaining windows, cursors, etc...
			glfwSetErrorCallback(null).free()
		}
	}

	private fun init(): Long {
		if (!glfwInit())
			throw IllegalStateException("Unable to initialize GLFW")

		glfwDefaultWindowHints()
		glfwWindowHint(GLFW_VISIBLE,   GLFW_FALSE) // hiding the window
		glfwWindowHint(GLFW_RESIZABLE, GLFW_FALSE) // window resizing not allowed
		glfwWindowHint(GLFW_CONTEXT_VERSION_MAJOR, 3)
		glfwWindowHint(GLFW_CONTEXT_VERSION_MINOR, 2)
		glfwWindowHint(GLFW_OPENGL_FORWARD_COMPAT, GLFW_TRUE)
		glfwWindowHint(GLFW_OPENGL_PROFILE, GLFW_OPENGL_CORE_PROFILE)

		val window = glfwCreateWindow(WIDTH, HEIGHT, "Flowerbox Client", NULL, NULL)
		if (window == NULL)
			throw RuntimeException("Failed to create the GLFW window")

		glfwSetKeyCallback(window, object : GLFWKeyCallback() {
			override fun invoke(window: Long, key: Int, scancode: Int, action: Int, mods: Int) {
				keyHandler(window, key, scancode, action, mods)
			}
		})
		glfwSetMouseButtonCallback(window, object : GLFWMouseButtonCallback() {
			override fun invoke(window: Long, button: Int, action: Int, mods: Int) {
				mouseHandler(window, button, action, mods)
			}
		})
		glfwSetCursorPosCallback(window, object : GLFWCursorPosCallback() {
			override fun invoke(window: Long, xpos: Double, ypos: Double) {
				cursorHandler(window, xpos, ypos)
			}
		})

		val vidMode = glfwGetVideoMode(glfwGetPrimaryMonitor())

		glfwSetWindowPos (
			window,
			(vidMode.width() -  WIDTH) / 2,
			(vidMode.height() - HEIGHT) / 2
		)

		glfwMakeContextCurrent(window)
		glfwSwapInterval(1)
		glfwShowWindow(window)

		return window
	}

	private fun loop(window: Long) {
		GL.createCapabilities()

		Renderer.setup(WIDTH, HEIGHT)

		while (!glfwWindowShouldClose(window)) {
			Renderer.render()
			glfwSwapBuffers(window)
			glfwPollEvents()
		}
	}

	fun keyHandler(window: Long, key: Int, scanCode: Int, action: Int, mods: Int) {
		println("key: $window, $key, $scanCode, $action, $mods")
		if (key == GLFW_KEY_ESCAPE && action == GLFW_RELEASE)
			glfwSetWindowShouldClose(window, true)
		/*if (key == GLFW_KEY_LEFT) {
			if (action == GLFW_PRESS || action == GLFW_REPEAT)
				Renderer.setRotate(-1f)
			else
				Renderer.setRotate(0f)
		}
		if (key == GLFW_KEY_RIGHT) {
			if (action == GLFW_PRESS || action == GLFW_REPEAT)
				Renderer.setRotate(1f)
			else
				Renderer.setRotate(0f)
		} */
	}

	private var mouseDown = false
	private var mouseDownX: Double = 0.0
	private var mouseDownY: Double = 0.0
	private var xrotDown: Float = 0f
	private var yrotDown: Float = 0f
	private var lastX: Double = 0.0
	private var lastY: Double = 0.0

	fun mouseHandler(window: Long, button: Int, action: Int, mods: Int) {
		println("mouse: $window, $button, $action, $mods")
		if (button == GLFW_MOUSE_BUTTON_1) {
			mouseDown = action == 1
			mouseDownX = lastX
			mouseDownY = lastY
			xrotDown = Renderer.xrot
			yrotDown = Renderer.yrot
		}
	}

	fun cursorHandler(window: Long, xpos: Double, ypos: Double) {
		lastX = xpos
		lastY = ypos

		if (mouseDown) {
			val deltaX = xpos - mouseDownX
			val deltaY = ypos - mouseDownY
			println("cursorDelta: $deltaX, $deltaY")

			Renderer.yrot = yrotDown - (deltaX / 4.0).toFloat()
			Renderer.xrot = xrotDown - (deltaY / 4.0).toFloat()
		}

		/*if (xpos < 0 || ypos < 0 || xpos >= WIDTH || ypos >= HEIGHT)
			return
		println("cursor: $window, $xpos, $ypos") */
	}
}
