/*
 * Copyright (C) 2017-2018 ObjectBox Ltd.
 *
 * This file is part of ObjectBox Build Tools.
 *
 * ObjectBox Build Tools is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * ObjectBox Build Tools is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with ObjectBox Build Tools.  If not, see <http://www.gnu.org/licenses/>.
 */

package io.objectbox.gradle.transform

import java.io.File

/**
 * Stores properties about a class (byte code) file to be used during transformation.
 *
 * @see ClassProber
 * @see ClassTransformer
 */
data class ProbedClass(
        /**
         * Directory to write the transformed class file into. Must be above the top-most package as subdirectories for
         * packages are auto-created by the transformer.
         * Background: Class files may have to be written to different directories (notably JavaCompile output dir is
         * different from KotlinCompile output dir).
         */
        val outDir: File,
        /** The file containing the byte-code for this class. */
        val file: File,
        val name: String,
        val javaPackage: String,
        val superClass: String? = null,
        val isCursor: Boolean = false,
        val isEntity: Boolean = false,
        val isEntityInfo: Boolean = false,
        val isBaseEntity: Boolean = false,
        /** Fully qualified names (dot notation) of generic types in fields of List (non-transient only) */
        val listFieldTypes: List<String> = emptyList(),
        val hasToOneRef: Boolean = false,
        val hasToManyRef: Boolean = false,
        val hasBoxStoreField: Boolean = false,
        val interfaces: List<String> = listOf()
) {
    fun hasRelation(entityTypes: Set<String>): Boolean
            = hasToOneRef || hasToManyRef || listFieldTypes.any { entityTypes.contains(it) }
}