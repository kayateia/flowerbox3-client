package net.kayateia.flowerbox.common.cherry.runtime.library

import net.kayateia.flowerbox.common.cherry.runtime.IntrinsicValue
import net.kayateia.flowerbox.common.cherry.runtime.NamespaceValue

class Library {
	val rootPackages = NamespaceValue("")

	init {
		add(Math.members)
	}

	fun add(members: List<IntrinsicImpl>) {
		for (m in members) {
			addOne(rootPackages, m.pkg, m)
		}
	}

	fun addOne(ns: NamespaceValue, pkgList: List<String>, impl: IntrinsicImpl) {
		if (pkgList.isEmpty()) {
			ns.map[impl.name] = IntrinsicValue(impl.impl)
		} else {
			val cur = pkgList.first()
			var curns: NamespaceValue? = ns.map[cur] as NamespaceValue?
			if (curns == null) {
				// TODO - Should actually check ns.has(cur) here in case there's a class already.
				curns = NamespaceValue(cur)
				ns.map[cur] = curns
			}
			addOne(curns, pkgList.drop(1), impl)
		}
	}
}
