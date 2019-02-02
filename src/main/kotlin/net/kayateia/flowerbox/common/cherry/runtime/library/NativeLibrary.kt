/*
	Flowerbox
	Copyright 2018 Kayateia

	This code is licensed under the GNU Public License version 3 or higher.
	Please see LICENSE.txt in the root of the project for more details.
 */

package net.kayateia.flowerbox.common.cherry.runtime.library

import net.kayateia.flowerbox.common.cherry.parser.AstProgram
import net.kayateia.flowerbox.common.cherry.runtime.ListValue
import net.kayateia.flowerbox.common.cherry.runtime.ObjectValue
import net.kayateia.flowerbox.common.cherry.runtime.Runtime
import net.kayateia.flowerbox.common.cherry.runtime.Value
import net.kayateia.flowerbox.common.cherry.runtime.scope.Scope

interface NativeObjectImpl {
	val members: List<NativeImpl>
	val declProgram: AstProgram
}

class NativeImpl(val namespace: String, val className: String, val memberName: String,
	val impl: suspend (runtime: Runtime, member: String, self: ObjectValue?, capturedScope: Scope, params: ListValue) -> Value)

class NativeClassMembers(val map: HashMap<String, NativeImpl> = HashMap())

class NativeNamespace(val map: HashMap<String,NativeClassMembers> = HashMap())

class NativeLibrary(val namespaces: HashMap<String, NativeNamespace> = HashMap()) {
	val allNativeImpls: List<NativeObjectImpl> = listOf(Debug, Math)

	init {
		for (i in allNativeImpls)
			registerAll(i.members)
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

	fun executeAllDecls(runtime: Runtime) {
		for (i in allNativeImpls)
			runtime.execute(i.declProgram)
	}
}
