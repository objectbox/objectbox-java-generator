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

package io.objectbox.reporting

import com.squareup.moshi.JsonClass
import com.squareup.moshi.JsonWriter
import com.squareup.moshi.Moshi
import okio.Buffer
import okio.sink
import java.io.File

@JsonClass(generateAdapter = true)
class ObjectBoxBuildConfig(
    val projectDir: String,
    val flavor: String? = null
) {
    companion object {
        const val FILE_NAME = "objectbox-build-config.json"

        /**
         * Returns a file in the given directory, ensures the directory exists.
         */
        fun buildFile(outputDir: File): File {
            if (!outputDir.exists()) outputDir.mkdirs()
            return File(outputDir, FILE_NAME)
        }
    }

    val timeStarted = System.currentTimeMillis()

    /**
     * Writes this class as JSON into the given file. Create the file with [buildFile].
     */
    fun writeInto(buildConfigFile: File) {
        val adapter = ObjectBoxBuildConfigJsonAdapter(Moshi.Builder().build())
        val buffer = Buffer()
        val jsonWriter = JsonWriter.of(buffer)
        jsonWriter.indent = "  "

        adapter.toJson(jsonWriter, this)

        buildConfigFile.sink().use {
            buffer.readAll(it)
        }
    }
}