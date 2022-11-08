package io.objectbox.gradle.transform

import com.google.common.truth.Truth.assertThat
import io.objectbox.gradle.GradleTestRunner
import javassist.ClassPool
import javassist.CtClass
import javassist.bytecode.ClassFile
import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.TaskOutcome
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.DataInputStream
import java.io.File
import java.lang.reflect.Modifier


/**
 * Generic implementation of an Android Plugin transform test.
 */
abstract class AndroidPluginTransformTest {

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
     * From project root, the path to the directory where transformed classes are written to.
     */
    abstract val buildTransformDirectory: String

    @Test
    fun assemble_transformRuns() {
        val gradleRunner = GradleTestRunner(testProjectDir)
            .apply {
                // Note: classpath for plugins configured in build script of this project (see GradleTestRunner.build).
                additionalPlugins += "com.android.application"
                additionalBlocks = buildScriptAndroidBlock
            }

        testProjectDir.newFile("src/main/AndroidManifest.xml").apply {
            writeText(androidManifest)
        }

        gradleRunner.addSourceFile(
            "ExampleEntity.java",
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
                    toManyProperty = new ToMany(this, ExampleEntity_.toManyProperty);
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
        )

        val result = gradleRunner.build(listOf("--stacktrace", "assembleDebug"), additionalRunnerConfiguration)
        assertThat(result.task(":assembleDebug")!!.outcome).isEqualTo(TaskOutcome.SUCCESS)

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