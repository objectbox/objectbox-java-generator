package org.greenrobot.greendao.codemodifier

import java.io.File

/**
 * Parses index spec from string, e.g.: "fieldA asc, fieldB DESC, fieldC"
 * */
fun parseIndexSpec(spec: String): List<OrderProperty> {
    require(spec.isNotBlank()) {
        "Index spec should not be empty"
    }
    return spec.split(',').map { it.trim() }.map { columnSpec ->
        require(columnSpec.length > 0, { "Wrong index spec: $spec" })
        val specPair = columnSpec.split(' ')
        when {
            specPair.size == 1 -> OrderProperty(specPair[0], Order.ASC)
            else -> OrderProperty(specPair[0], Order.valueOf(specPair[1].toUpperCase()))
        }
    }
}

fun String.nullIfBlank() : String? = if (isBlank()) null else this

/** More optimized than standard groupBy{}.maxBy{} */
fun <T> Sequence<T>.mostPopular(): T? {
    val counts = mutableMapOf<T, Int>()
    for (t in this) counts.put(t, counts.getOrElse(t, {0}) + 1)
    return counts.asSequence().maxBy { it.value }?.key
}

fun File.getJavaClassNames(): List<String> {
    require(this.isDirectory) { "The file should be a directory" }
    return list().filter { it.endsWith(".java", ignoreCase = true) }
        .map { File(this, it) }
        .filter { it.isFile }
        .map { it.nameWithoutExtension }
}

/** Measure block execution time */
inline fun <T> logTime(action: String, block: () -> T) : T {
    val start = System.currentTimeMillis()
    val result = block()
    val time = System.currentTimeMillis() - start;
    println("$action took $time ms")
    return result
}
