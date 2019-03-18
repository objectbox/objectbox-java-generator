package io.objectbox.reporting

import java.io.File
import java.io.FileReader
import java.io.FileWriter
import java.io.IOException
import java.lang.UnsupportedOperationException
import java.util.*


/**
 * Reads [Properties] from and stores them in a file in the user directory
 * or alternatively the temporary files directory.
 */
class BuildPropertiesFile {

    private val file = try {
        val dir = File(System.getProperty("user.home"))
        if (dir.isDirectory) {
            File(dir, FILE_NAME)
        } else {
            throw UnsupportedOperationException("user.home is not a directory")
        }
    } catch (e: Exception) {
        System.err.println("Could not get user dir: $e") // No stack trace
        File(System.getProperty("java.io.tmpdir"), FILE_NAME) // Plan B
    }

    val properties: Properties

    init {
        var propertiesTemp = Properties()
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
        properties = propertiesTemp
    }

    fun write() {
        FileWriter(file).use {
            properties.store(it, "Properties for ObjectBox build tools")
        }
    }

    private companion object {
        private const val FILE_NAME = ".objectbox-build.properties"
    }

}