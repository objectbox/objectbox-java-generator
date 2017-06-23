package io.objectbox.gradle.transform

class ClassTransformerStats {
    val startTime = System.currentTimeMillis()
    var endTime = 0L
    val time get() = if (endTime > 0) endTime - startTime else throw RuntimeException("Not finished yet")

    var countTransformed = 0
    var countCopied = 0

    var toOnesFound = 0
    var toOnesInitialized = 0

    fun done() {
        endTime = System.currentTimeMillis()
        System.out.println("Transformed $countTransformed entities and copied $countCopied classes in $time ms")

    }
}
