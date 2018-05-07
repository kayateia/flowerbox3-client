/*
	Flowerbox
	Copyright 2018 Kayateia

	This code is licensed under the MIT license; please see LICENSE.md in the root of the project.
 */

package net.kayateia.flowerbox.common.cherry.runtime.library

import net.kayateia.flowerbox.common.cherry.runtime.ListValue
import net.kayateia.flowerbox.common.cherry.runtime.Runtime
import net.kayateia.flowerbox.common.cherry.runtime.Value
import net.kayateia.flowerbox.common.cherry.runtime.scope.Scope

class NativeImpl(val namespace: String, val className: String, val memberName: String,
	val impl: suspend (runtime: Runtime, member: String, self: HashMap<String, Value>?, capturedScope: Scope, params: ListValue) -> Value)

class NativeClassMembers(val map: HashMap<String, NativeImpl> = HashMap())

class NativeNamespace(val map: HashMap<String,NativeClassMembers> = HashMap())

class NativeLibrary(val namespaces: HashMap<String, NativeNamespace> = HashMap()) {
	init {
		registerAll(Debug.members)
		registerAll(Math.members)
	}

	fun register(native: NativeImpl) {
		val nsMap = if (namespaces.containsKey(native.namespace))
				namespaces[native.namespace]!!
			else {
				val newNs = NativeNamespace()
				namespaces[native.namespace] = newNs
				newNs
			}
		// Don't know why this doesn't work... .put() doesn't return the value it just put in?
		// val nsMap = (namespaces[native.namespace] ?: namespaces.put(native.namespace, NativeNamespace()))

		val nsClass = if (nsMap.map.containsKey(native.className))
				nsMap.map[native.className]!!
			else {
				val newClass = NativeClassMembers()
				nsMap.map[native.className] = newClass
				newClass
			}

		nsClass.map[native.memberName] = native
	}

	fun registerAll(nativeList: List<NativeImpl>) {
		for (i in nativeList)
			register(i)
	}
}
