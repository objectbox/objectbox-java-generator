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

import io.objectbox.logging.log

/**
 * Used by [ClassTransformer] to store and log some statistics about the transform process. Useful for testing.
 */
class ClassTransformerStats {
    val startTime = System.currentTimeMillis()
    var endTime = 0L
    val time get() = if (endTime > 0) endTime - startTime else throw RuntimeException("Not finished yet")

    var countTransformed = 0
    var countCopied = 0

    var toOnesFound = 0
    var toOnesInitializerAdded = 0
    var constructorsCheckedForTransform = 0

    var toManyFound = 0
    var toManyInitializerAdded = 0

    var boxStoreFieldsMadeVisible = 0
    var boxStoreFieldsAdded = 0

    fun done() {
        endTime = System.currentTimeMillis()
        log("Transformed $countTransformed entities and copied $countCopied classes in $time ms")
    }

}
