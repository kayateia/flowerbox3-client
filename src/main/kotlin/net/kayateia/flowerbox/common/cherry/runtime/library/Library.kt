/*
	Flowerbox
	Copyright 2018 Kayateia

	Please see LICENSE.txt in the root of the project for more details.
 */

package net.kayateia.flowerbox.common.cherry.runtime.library

import net.kayateia.flowerbox.common.cherry.runtime.NamespaceValue
import net.kayateia.flowerbox.common.cherry.runtime.Value

class Library {
	val rootPackages = NamespaceValue("")

	// Looks for an item in the namespace hierarchy, returning null if it's not found.
	// The items specified in "usings" will be tried as prefixes.
	fun lookup(className: String, usings: List<String>): Value? {
		// First check to see if we have a fully qualified name or not. If not, we'll try various
		// "using'd" namespaces first.
		if (!className.contains('.')) {
			// Is it in the root namespace?
			if (rootPackages.map.containsKey(className))
				return rootPackages.map[className]

			for (u in usings) {
				val result = lookup("$u.$className")
				if (result != null)
					return result
			}

			return null
		} else
			return lookup(className)
	}

	// Looks for an item in the namespace hierarchy, returning null if it's not found.
	fun lookup(fqcn: String): Value? {
		val parts = fqcn.split(".")
		var cur: Value? = rootPackages
		for (i in parts) {
			val next = when (cur) {
				is NamespaceValue -> cur.map[i]
				else -> return null
			}
			cur = next
		}

		return cur
	}
}
