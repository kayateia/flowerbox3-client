/*
	Flowerbox
	Copyright 2018 Kayateia

	This code is licensed under the MIT license; please see LICENSE.md in the root of the project.
 */

package net.kayateia.flowerbox.common.world.cherry

import net.kayateia.flowerbox.common.cherry.objects.CherryNativeCompanion
import net.kayateia.flowerbox.common.cherry.parser.AstProgram
import net.kayateia.flowerbox.common.cherry.runtime.Runtime

interface WorldCherryItem : CherryNativeCompanion {
	val codeParsed: AstProgram
}

object WorldCherryLibrary {
	val toRegister: List<WorldCherryItem> = listOf(
		NativeVector
	)

	fun registerAll(runtime: Runtime) {
		for (i in toRegister) {
			i.cherrySetup(runtime)
			runtime.execute(i.codeParsed)
		}
	}
}
