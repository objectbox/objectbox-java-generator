package io.objectbox.gradle

import java.io.File

data class ProbedEntity(
        val file: File,
        val name: String,
        val hasToOne: Boolean,
        val hasToMany: Boolean,
        val hasBoxStoreField: Boolean
)