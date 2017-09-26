package io.objectbox.codemodifier

fun String.nullIfBlank() : String? = if (isBlank()) null else this
