package io.objectbox.gradle.transform

import java.io.File

data class ProbedClass(
        val file: File,
        val name: String,
        val javaPackage: String,
        val isCursor: Boolean = false,
        val isEntity: Boolean = false,
        val hasToOne: Boolean = false,
        val hasToMany: Boolean = false,
        val hasBoxStoreField: Boolean= false
)