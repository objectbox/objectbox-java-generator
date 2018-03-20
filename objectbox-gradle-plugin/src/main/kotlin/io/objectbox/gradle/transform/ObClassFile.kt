package io.objectbox.gradle.transform

import java.io.File

/** Stores the full path to a class file and the directory a transformed copy should be written to (directory path does
 * not include directories for packages). */
data class ObClassFile(val outDir: File, val file: File)