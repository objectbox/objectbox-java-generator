/*
 * ObjectBox Build Tools
 * Copyright (C) 2017-2024 ObjectBox Ltd.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published
 * by the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package io.objectbox.generator.model

import io.objectbox.model.PropertyFlags

class Index(property: Property, indexFlags: Int) {

    val properties: MutableList<Property>

    /**
     * One or more INDEX and UNIQUE [PropertyFlags].
     */
    val indexFlags: Int

    /** Used to restrict index value length for String and byte[] if using value based index. */
    var maxValueLength = 0

    val isUnique: Boolean
        get() {
            return indexFlags.hasFlag(PropertyFlags.UNIQUE)
        }

    val isUniqueOnConflictReplace: Boolean
        get() {
            return indexFlags.hasFlag(PropertyFlags.UNIQUE_ON_CONFLICT_REPLACE)
        }

    init {
        require(indexFlags != 0) { "Index flags must be one or more INDEX or UNIQUE PropertyFlags, was $indexFlags." }
        properties = ArrayList()
        addProperty(property)
        this.indexFlags = indexFlags
    }

    constructor(property: Property) : this(property, PropertyFlags.INDEXED)

    // Currently unused, keep for potential future support of indexes over multiple properties.
    @Suppress("MemberVisibilityCanBePrivate")
    fun addProperty(property: Property) {
        properties.add(property)
    }

    private val flagsToNames = mapOf(
        PropertyFlags.INDEXED to "PropertyFlags.INDEXED",
        PropertyFlags.INDEX_HASH to "PropertyFlags.INDEX_HASH",
        PropertyFlags.INDEX_HASH64 to "PropertyFlags.INDEX_HASH64",
        PropertyFlags.UNIQUE to "PropertyFlags.UNIQUE",
        PropertyFlags.UNIQUE_ON_CONFLICT_REPLACE to "PropertyFlags.UNIQUE_ON_CONFLICT_REPLACE",
    )

    fun getIndexFlagsAsNames(): List<String> {
        val names = mutableListOf<String>()
        flagsToNames.forEach {
            if (indexFlags.hasFlag(it.key)) names.add(it.value)
        }
        return names
    }

    private fun Int.hasFlag(flag: Int): Boolean = this and flag != 0

}