package io.objectbox.reporting

import io.objectbox.reporting.BuildPropertiesFile.FileCreateListener
import java.io.File
import java.io.FileReader
import java.io.FileWriter
import java.io.IOException
import java.util.*


/**
 * Reads [Properties] from and stores them in a file in the user directory.
 * If the file can't be created calls the given [FileCreateListener].
 */
class BuildPropertiesFile(fileCreateListener: FileCreateListener) {

    interface FileCreateListener {
        fun onFailedToCreateFile(message: String, e: Exception)
    }

    private val file = try {
        val dir = File(System.getProperty("user.home"))
        if (dir.isDirectory) {
            File(dir, FILE_NAME)
        } else {
            throw UnsupportedOperationException("user.home is not a directory")
        }
    } catch (e: Exception) {
        val message = "Could not get user dir: $e"
        System.err.println(message) // No stack trace
        fileCreateListener.onFailedToCreateFile(message, e)
        null
    }

    val hasNoFile = file == null

    val properties: Properties

    init {
        var propertiesTemp = Properties()
        file?.let { file ->
            if (file.exists()) {
                try {
                    FileReader(file).use {
                        propertiesTemp.load(it)
                    }
                } catch (e: IOException) {
                    e.printStackTrace()
                    propertiesTemp = Properties()
                }
            }
        }
        properties = propertiesTemp
    }

    fun write() {
        file?.let { file ->
            FileWriter(file).use {
                properties.store(it, "Properties for ObjectBox build tools")
            }
        }
    }

    private companion object {
        private const val FILE_NAME = ".objectbox-build.properties"
    }

}