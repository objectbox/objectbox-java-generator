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

package io.objectbox.processor

import io.objectbox.reporting.ObjectBoxBuildConfig
import java.io.File
import java.io.FileNotFoundException
import java.io.IOException
import java.net.URI
import java.net.URISyntaxException
import javax.annotation.processing.Filer
import javax.tools.FileObject
import javax.tools.StandardLocation

/**
 * We use a dirty trick to find the model file, since it's not
 * available in the classpath. The idea is quite simple: create a fake
 * class file, retrieve its URI, and start going up in parent folders.
 * Any better solution will be appreciated.
 *
 * Sourced from AndroidAnnotations and http://stackoverflow.com/a/37230331.
 *
 * Might not be necessary in the future: https://issuetracker.google.com/issues/37063033
 */
@Throws(FileNotFoundException::class)
fun findProjectRoot(filer: Filer): File {
    val fileProbe: FileObject
    try {
        fileProbe = filer.createResource(
            StandardLocation.SOURCE_OUTPUT, "",
            "objectbox-probe" + System.currentTimeMillis()
        )
    } catch (e: IOException) {
        throw FileNotFoundException("probe failed: ${e.message}")
    }

    var filePathProbe = fileProbe.toUri().toString()

    // unit test support: handle compile-testing InMemoryJavaFileManager
    if (filePathProbe.startsWith("mem://")) {
        // use a fall-back path: check if the current absolute path contains a build folder, then use that path
        val buildFolder = File("build").absoluteFile
        if (buildFolder.isDirectory) {
            return buildFolder.parentFile
        }
    }

    if (filePathProbe.startsWith("file:")) {
        if (!filePathProbe.startsWith("file://")) {
            filePathProbe = "file://" + filePathProbe.substring("file:".length)
        }
    } else {
        filePathProbe = "file://$filePathProbe"
    }

    val cleanURI: URI
    try {
        cleanURI = URI(filePathProbe)
    } catch (e: URISyntaxException) {
        throw FileNotFoundException("parse failed: ${e.message}")
    }

    // going up structures like build/generated/source/apt/debug (+flavor, ...) until we hit our file or "build/"
    var dir = File(cleanURI)
    var buildDir: File? = null
    var buildDirWithoutConfigFile: File? = null
    for (i in 1..9) {
        dir = dir.parentFile ?: break
        if (File(dir, ObjectBoxBuildConfig.FILE_NAME).exists()) {
            buildDir = dir
            break
        } else if (dir.name == "build" && buildDirWithoutConfigFile == null) {
            buildDirWithoutConfigFile = dir
        }
    }

    return buildDir?.parentFile ?: buildDirWithoutConfigFile?.parentFile
    ?: throw FileNotFoundException("Could not determine build folder from $filePathProbe")
}