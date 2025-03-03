/*
 * ObjectBox Build Tools
 * Copyright (C) 2017-2025 ObjectBox Ltd.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published
 * by the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package io.objectbox.gradle.transform

import org.junit.Assert
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import kotlin.reflect.KClass

abstract class AbstractTransformTest {
    private val classDirs = arrayOf(
        // See also: https://docs.gradle.org/4.0/release-notes.html#location-of-classes-in-the-build-directory
        "build/classes/kotlin/testFixtures",
        "objectbox-gradle-plugin/build/classes/kotlin/testFixtures",
    )
    private val classDir = classDirs.map(::File).first { it.exists() }

    private val prober = ClassProber()

    private val transformer = ClassTransformer(true)

    @Test
    fun testClassDir() {
        assertTrue(classDir.absolutePath, classDir.exists())
    }

    protected fun probeClass(kclass: KClass<*>, outDir: File = File(".")): ProbedClass {
        val file = File(classDir, kclass.qualifiedName!!.replace('.', '/') + ".class")
        assertTrue(file.absolutePath, file.exists())
        return prober.probeClass(file, outDir)
    }

    fun testTransformOrCopy(kClass: KClass<*>, expectedTransformed: Int, expectedCopied: Int) =
        testTransformOrCopy(listOf(kClass), expectedTransformed, expectedCopied)

    fun testTransformOrCopy(kClasses: List<KClass<*>>, expectedTransformed: Int, expectedCopied: Int)
            : Pair<ClassTransformerStats, List<File>> {
        val tempDir = File.createTempFile(this.javaClass.name, "")
        tempDir.delete()
        assertTrue(tempDir.mkdir())
        val probedClasses = kClasses.map { probeClass(it, tempDir) }
        try {
            val stats = transformer.transformOrCopyClasses(probedClasses)
            Assert.assertEquals(expectedTransformed, stats.countTransformed)
            Assert.assertEquals(expectedCopied, stats.countCopied)
            val createdFiles = tempDir.walkBottomUp().toList().filter { it.isFile }
            Assert.assertEquals(expectedTransformed + expectedCopied, createdFiles.size)
            return Pair(stats, createdFiles)
        } finally {
            tempDir.deleteRecursively()
        }
    }

}
