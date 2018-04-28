package net.kayateia.flowerbox.common.cherry.runtime.library

import net.kayateia.flowerbox.common.cherry.runtime.ArrayValue
import net.kayateia.flowerbox.common.cherry.runtime.Runtime
import net.kayateia.flowerbox.common.cherry.runtime.Value
import net.kayateia.flowerbox.common.cherry.runtime.scope.Scope

class IntrinsicImpl(val pkg: List<String>, val name: String, val impl: (runtime: Runtime, implicits: Scope, params: ArrayValue) -> Value)
