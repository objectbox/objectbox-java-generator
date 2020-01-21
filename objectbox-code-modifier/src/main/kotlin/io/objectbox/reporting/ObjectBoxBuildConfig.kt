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

package io.objectbox.reporting

import com.squareup.moshi.JsonWriter
import com.squareup.moshi.Moshi
import okio.Buffer
import okio.Okio
import java.io.File

class ObjectBoxBuildConfig(val projectDir: String, val flavor: String? = null) {
    companion object {
        const val FILE_NAME = "objectbox-build-config.json"
    }

    val timeStarted = System.currentTimeMillis()

    /**
     * Writes this class as JSON into a file inside the given directory.
     * The file is named [FILE_NAME].
     */
    fun writeInto(folder: File) {
        val adapter = Moshi.Builder().build().adapter<ObjectBoxBuildConfig>(ObjectBoxBuildConfig::class.java)
        val buffer = Buffer()
        val jsonWriter = JsonWriter.of(buffer)
        jsonWriter.indent = "  "
        adapter.toJson(jsonWriter, this)

        Okio.sink(File(folder, FILE_NAME)).use {
            buffer.readAll(it)
        }
    }
}