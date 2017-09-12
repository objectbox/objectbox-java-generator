package io.objectbox.gradle.transform

import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import kotlin.reflect.KClass

abstract class AbstractTransformTest {
    private val classDirs = arrayOf(
            // Old Gradle style (multiple languages into same classes dir)
            "build/classes/test",
            "objectbox-gradle-plugin/build/classes/test",
            // See also: https://docs.gradle.org/4.0/release-notes.html#location-of-classes-in-the-build-directory
            "build/classes/java/test", // <-- Gradle 4.0 change, may change /kotlin/ with a future Kotlin version
            "objectbox-gradle-plugin/build/classes/java/test",
            // IntelliJ uses "out"
            "out/test/classes/",
            "objectbox-gradle-plugin/out/test/classes/"
    )
    val classDir = classDirs.map(::File).first { it.exists() }

    val prober = ClassProber()

    @Test
    fun testClassDir() {
        assertTrue(classDir.absolutePath, classDir.exists())
    }

    protected fun probeClass(kclass: KClass<*>): ProbedClass {
        val file = File(classDir, kclass.qualifiedName!!.replace('.', '/') + ".class")
        assertTrue(file.absolutePath, file.exists())
        return prober.probeClass(file)
    }

}
