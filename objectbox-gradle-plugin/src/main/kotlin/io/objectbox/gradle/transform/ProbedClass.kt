package io.objectbox.gradle.transform

import java.io.File

data class ProbedClass(
        val file: File,
        val name: String,
        val javaPackage: String,
        val superClass: String? = null,
        val isCursor: Boolean = false,
        val isEntity: Boolean = false,
        /** Fully qualified names (dot notation) of generic types in fields of List (non-transient only) */
        var listFieldTypes: List<String> = emptyList(),
        var hasToOneRef: Boolean = false,
        var hasToManyRef: Boolean = false,
        var hasBoxStoreField: Boolean = false,
        val isEntityInfo: Boolean = false
) {
    fun hasRelation(entityTypes: Set<String>): Boolean
            = hasToOneRef || hasToManyRef || listFieldTypes.any { entityTypes.contains(it) }
}