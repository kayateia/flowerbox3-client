/*
	Flowerbox
	Copyright 2018 Kayateia

	This code is licensed under the MIT license; please see LICENSE.md in the root of the project.
 */

package net.kayateia.flowerbox.common.cherry.runtime.step.ast

import net.kayateia.flowerbox.common.cherry.runtime.Runtime
import net.kayateia.flowerbox.common.cherry.runtime.step.Step
import net.kayateia.flowerbox.common.cherry.runtime.step.helper.OpPopper

abstract class Statement : Step {
	override fun execute(runtime: Runtime) {
		val popper = OpPopper()
		runtime.opPush(popper)
		runtime.codePush(popper)
	}
}
