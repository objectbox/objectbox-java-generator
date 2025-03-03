/*
 * ObjectBox Build Tools
 * Copyright (C) 2022-2025 ObjectBox Ltd.
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

import com.android.build.api.instrumentation.ClassContext
import com.android.build.api.instrumentation.ClassData
import com.google.common.truth.Truth.assertThat
import io.objectbox.annotation.Entity
import org.junit.Test
import org.objectweb.asm.ClassReader
import org.objectweb.asm.Opcodes
import java.io.File
import kotlin.reflect.KClass

/**
 * Tests covering the ASM byte-code transformer. Note that there are similar tests
 * for the Javassist-based byte-code transformer in the code-modifier project.
 */
class ObjectBoxAsmClassVisitorTest {

    @Test
    fun entity_isTransformed() {
        val classData = object : ClassData {
            override val classAnnotations: List<String> = listOf(Entity::class.qualifiedName!!)
            override val className: String = ExampleEntity::class.qualifiedName!!
            override val interfaces: List<String> = emptyList()
            override val superClasses: List<String> = emptyList()
        }
        val classContext = object : ClassContext {
            override val currentClassData = classData

            override fun loadClassData(className: String): ClassData? {
                return if (className == ExampleEntity::class.qualifiedName) classData else null
            }
        }

        val transformer = transform(ExampleEntity::class, classContext)
        transformer.stats.also {
            // Data class no-param constructor and special Kotlin constructor call other constructor,
            // should not be transformed.
            assertThat(it.constructorsCheckedForTransform).isEqualTo(1)
            assertThat(it.boxStoreFieldsAdded).isEqualTo(1)
            assertThat(it.toOnesFound).isEqualTo(1)
            assertThat(it.toManyFound).isEqualTo(2)
            assertThat(it.toOnesInitializerAdded).isEqualTo(1)
            // toManyProperty is already initialized, should only init toManyListProperty
            assertThat(it.toManyInitializerAdded).isEqualTo(1)
        }
    }

    @Test
    fun cursor_isTransformed() {
        val transformer = transformCursor(TestCursor::class)
        transformer.stats.also {
            assertThat(it.countTransformed).isEqualTo(1)
        }
    }

    @Test
    fun cursorAttachReads_isTransformed() {
        val transformer = transformCursor(CursorExistingImplReads::class)
        transformer.stats.also {
            assertThat(it.countTransformed).isEqualTo(1)
        }
    }

    @Test
    fun cursorAttachWrites_notTransformed() {
        val transformer = transformCursor(CursorExistingImplWrites::class)
        transformer.stats.also {
            assertThat(it.countTransformed).isEqualTo(0)
        }
    }

    private fun transformCursor(kClass: KClass<*>): ObjectBoxAsmClassVisitor {
        val classData = object : ClassData {
            override val classAnnotations: List<String> = emptyList()
            override val className: String = kClass::class.qualifiedName!!
            override val interfaces: List<String> = emptyList()
            override val superClasses: List<String> = emptyList()
        }
        val classContext = object : ClassContext {
            override val currentClassData = classData

            override fun loadClassData(className: String): ClassData? {
                return null
            }
        }
        return transform(kClass, classContext)
    }

    private fun transform(kClass: KClass<*>, classContext: ClassContext): ObjectBoxAsmClassVisitor {
        // Test classes are provided via a Gradle test fixture of objectbox-code-modifier,
        // look in the directory its files are compiled into.
        val classFile = File(
            "../objectbox-code-modifier/build/classes/kotlin/testFixtures",
            kClass.qualifiedName!!.replace(".", "/") + ".class"
        )
        val classReader = ClassReader(classFile.inputStream())
        val transformer = ObjectBoxAsmClassVisitor(Opcodes.ASM9, null, classContext, true)
        classReader.accept(transformer, 0)
        return transformer
    }

}