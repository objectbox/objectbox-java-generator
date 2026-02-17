/*
 * ObjectBox Build Tools
 * Copyright (C) 2022-2024 ObjectBox Ltd.
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

package io.objectbox.gradle

import com.google.common.truth.Truth.assertThat
import io.objectbox.gradle.transform.ClassConst
import io.objectbox.gradle.transform.assignsBoxStoreField
import io.objectbox.gradle.transform.getInitializedFields
import javassist.ClassPool
import javassist.CtClass
import javassist.bytecode.ClassFile
import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.TaskOutcome
import org.intellij.lang.annotations.Language
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.DataInputStream
import java.io.File
import java.lang.reflect.Modifier


/**
 * Generic implementation to test the plugin with an Android project.
 */
abstract class AndroidProjectPluginTest {

    @JvmField
    @Rule
    val testProjectDir: TemporaryFolder = TemporaryFolder.builder().assureDeletion().build()

    abstract val buildScriptAndroidBlock: String

    abstract val androidManifest: String

    open val additionalRunnerConfiguration: ((GradleRunner) -> Unit) = {
        // Log build output to standard out, does not pollute logs as just using --stacktrace option.
        it.forwardOutput()
    }

    /**
     * Used to check that build tracker detects the correct Android Gradle Plugin version.
     */
    abstract val expectedAndroidPluginVersion: String

    /**
     * Used to check that build tracker detects the correct Gradle version.
     */
    abstract val expectedGradleVersion: String

    /**
     * From project root, the path to the directory where transformed classes are written to.
     */
    abstract val buildTransformDirectory: String

    @Test
    fun assemble() {
        val gradleRunner = GradleTestRunner(testProjectDir)
            .apply {
                // Note: classpath for plugins configured in build script of this project (see GradleTestRunner.build).
                additionalPlugins += "com.android.application"
                additionalBlocks = buildScriptAndroidBlock
            }

        testProjectDir.newFile("src/main/AndroidManifest.xml").apply {
            writeText(androidManifest)
        }

        @Language("Java")
        val exampleEntitySource =
            """
            package com.example;
            
            import io.objectbox.annotation.Convert;
            import io.objectbox.annotation.Entity;
            import io.objectbox.annotation.Id;
            import io.objectbox.annotation.Transient;
            import io.objectbox.converter.PropertyConverter;
            import io.objectbox.relation.ToMany;
            import io.objectbox.relation.ToOne;
            import java.util.List;
            
            @Entity
            public class ExampleEntity {
                @Id public long id;
                
                public ToOne<ExampleEntity> toOneProperty;
                public ToMany<ExampleEntity> toManyProperty;
                public List<ExampleEntity> toManyListProperty;
                
                public transient ToOne<ExampleEntity> transientProperty;
                @Transient public ToMany<ExampleEntity> transientProperty2;
                @Convert(converter = TestConverter.class, dbType = String.class)
                public List<ExampleEntity> convertProperty;
                
                public ExampleEntity(String log) {
                    toManyProperty = new ToMany<>(this, ExampleEntity_.toManyProperty);
                    System.out.println(log);
                }
                
                public ExampleEntity() {
                    this("calls other constructor");
                }
                
                public static class TestConverter implements PropertyConverter<List<ExampleEntity>, String> {
                    @Override
                    public List<ExampleEntity> convertToEntityProperty(String databaseValue) {
                        return null;
                    }
                    @Override
                    public String convertToDatabaseValue(List<ExampleEntity> entityProperty) {
                        return null;
                    }
                }
            } 
            """.trimIndent()

        gradleRunner.addSourceFile("ExampleEntity.java", exampleEntitySource)

        val result = gradleRunner.build(listOf("--stacktrace", "assembleDebug"), additionalRunnerConfiguration)
        assertThat(result.task(":assembleDebug")!!.outcome).isEqualTo(TaskOutcome.SUCCESS)

        // Assert build tracker.
        @Suppress("RegExpRedundantEscape")
        val buildTrackerLog = "\\[ObjectBox\\] Analytics disabled, would have sent event: .*".toRegex()
            .find(result.output)?.value
        assertThat(buildTrackerLog).contains("\"event\": \"Build\"")
        assertThat(buildTrackerLog).contains("\"Tool\": \"GradlePlugin\"")
        assertThat(buildTrackerLog).contains("\"Target\": \"Android\"")
        assertThat(buildTrackerLog).contains("\"AGP\": \"$expectedAndroidPluginVersion\"")
        assertThat(buildTrackerLog).contains("\"Gradle\": \"$expectedGradleVersion\"")

        // Assert transform output.
        val transformDir = File(testProjectDir.root, buildTransformDirectory)

        // Check entity is transformed.
        File(transformDir, "com/example/ExampleEntity.class").inputStream().use { fileInputStream ->
            val classFile = ClassFile(DataInputStream(fileInputStream))

            // Check BoxStore field exists and is accessible.
            val boxStoreField = classFile.fields.find { it.name == ClassConst.boxStoreFieldName }
            assertThat(boxStoreField).isNotNull()
            assertThat(Modifier.isPrivate(boxStoreField!!.accessFlags)).isFalse()
            assertThat(Modifier.isTransient(boxStoreField.accessFlags)).isTrue()

            // Check constructors not calling other constructors initialize relation properties.
            val constructors = classFile.methods.filter { it.isConstructor }
            assertThat(constructors).hasSize(2)
            constructors.forEach {
                val callsSuper = it.codeAttribute.iterator().skipSuperConstructor() != -1
                val initializedFields = classFile.getInitializedFields(it)
                if (callsSuper) {
                    // Constructor calls super()
                    assertThat(initializedFields).containsExactly(
                        "toOneProperty",
                        "toManyProperty",
                        "toManyListProperty"
                    )
                } else {
                    // Constructor calls this()
                    assertThat(initializedFields).isEmpty()
                }
            }
        }

        // Check Cursor is transformed.
        File(transformDir, "com/example/ExampleEntityCursor.class").inputStream().use { fileInputStream ->
            val pool: ClassPool = ClassPool.getDefault()
            val ctClass: CtClass = pool.makeClass(fileInputStream)
            val attachMethod = ctClass.declaredMethods.first { it.name == ClassConst.cursorAttachEntityMethodName }
            assertThat(attachMethod.assignsBoxStoreField()).isTrue()
        }

    }

}