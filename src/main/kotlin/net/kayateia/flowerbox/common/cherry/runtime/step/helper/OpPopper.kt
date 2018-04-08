/*
	Flowerbox
	Copyright 2018 Kayateia

	This code is licensed under the MIT license; please see LICENSE.md in the root of the project.
 */

package net.kayateia.flowerbox.common.cherry.runtime.step.helper

import net.kayateia.flowerbox.common.cherry.runtime.Runtime
import net.kayateia.flowerbox.common.cherry.runtime.step.Step

class OpPopper : Step {
	override fun execute(runtime: Runtime) {
		var last: Any? = null
		do {
			last = runtime.opPop()
		} while (last != this)
	}
}
