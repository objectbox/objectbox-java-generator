package io.objectbox.gradle.transform

import java.io.File

data class ProbedClass(
        val file: File,
        val name: String,
        val javaPackage: String,
        val superClass: String? = null,
        val isCursor: Boolean = false,
        val isEntity: Boolean = false,
        val isEntityInfo: Boolean = false,
        val isBaseEntity: Boolean = false,
        /** Fully qualified names (dot notation) of generic types in fields of List (non-transient only) */
        val listFieldTypes: List<String> = emptyList(),
        val hasToOneRef: Boolean = false,
        val hasToManyRef: Boolean = false,
        val hasBoxStoreField: Boolean = false,
        val interfaces: List<String> = listOf()
) {
    fun hasRelation(entityTypes: Set<String>): Boolean
            = hasToOneRef || hasToManyRef || listFieldTypes.any { entityTypes.contains(it) }
}