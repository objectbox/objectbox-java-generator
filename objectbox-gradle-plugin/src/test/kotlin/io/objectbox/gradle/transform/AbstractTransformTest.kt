package io.objectbox.gradle.transform

import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import kotlin.reflect.KClass

abstract class AbstractTransformTest {
    private val classDirs = arrayOf(
            "build/classes/test",
            "objectbox-gradle-plugin/build/classes/test",
            "out/test/classes/",
            "objectbox-gradle-plugin/out/test/classes/"
    )
    val classDir = classDirs.map(::File).first { it.exists() }

    val prober = ClassProber(true)

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
