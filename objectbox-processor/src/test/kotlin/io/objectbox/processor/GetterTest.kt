package io.objectbox.processor

import com.google.common.truth.Truth
import com.google.testing.compile.CompilationSubject
import com.google.testing.compile.JavaFileObjects
import org.junit.Test

/** Tests related to detecting getter methods of properties. */
class GetterTest : BaseProcessorTest() {

    @Test
    fun isGetter_nonBooleanProperty_isDetected() {
        val source = """
        package com.example.objectbox;
        import io.objectbox.annotation.Entity;
        import io.objectbox.annotation.Id;
        import org.jetbrains.annotations.Nullable;

        @Entity
        public class Example {
            @Id long id;
            
            // Kotlin equivalent:
            // var isProperty: Integer? = null
            
            @Nullable
            private Integer isProperty;
            @Nullable
            public final Integer isProperty() {
                return this.isProperty;
            }
        }
        """
        val javaFileObject = JavaFileObjects.forSourceString("com.example.objectbox.Example", source)

        val environment = TestEnvironment("getter-is-temp.json")

        val compilation = environment.compile(listOf(javaFileObject))
        CompilationSubject.assertThat(compilation).succeededWithoutWarnings()

        val entity = environment.schema.entities[0]!!
        val property = entity.properties!!.find { it.propertyName == "isProperty" }!!
        Truth.assertThat(property.getterMethodName).isEqualTo("isProperty")
    }

}