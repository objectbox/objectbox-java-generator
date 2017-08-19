package io.objectbox.processor

import io.objectbox.build.ObjectBoxBuildConfig
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
        fileProbe = filer.createResource(StandardLocation.SOURCE_OUTPUT, "",
                "objectbox-probe" + System.currentTimeMillis())
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
        filePathProbe = "file://" + filePathProbe
    }

    val cleanURI: URI
    try {
        cleanURI = URI(filePathProbe)
    } catch (e: URISyntaxException) {
        throw FileNotFoundException("parse failed: ${e.message}")
    }

    // going up structures like build/generated/source/apt/debug (+flavor, ...) until we hit our file
    var dir = File(cleanURI)
    var buildDir : File? = null
    for (i in 1..9) {
        dir = dir.parentFile
        if(File(dir, ObjectBoxBuildConfig.FILE_NAME).exists()) {
            buildDir = dir
            break
        }
    }

    return buildDir?.parentFile ?: throw FileNotFoundException("Could not determine build folder from $fileProbe")
}