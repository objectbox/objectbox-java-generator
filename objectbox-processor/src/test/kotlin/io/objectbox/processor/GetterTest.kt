package io.objectbox.processor

import com.google.common.truth.Truth
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

        val environment = TestEnvironment("getter-is.json", useTemporaryModelFile = true)

        environment.compile(listOf(javaFileObject))
            .assertThatIt { succeededWithoutWarnings() }

        val entity = environment.schema.entities[0]!!
        val property = entity.properties!!.find { it.propertyName == "isProperty" }!!
        Truth.assertThat(property.getterMethodName).isEqualTo("isProperty")
    }

    /**
     * Instead of compile error due to return type mismatch on is-getter, use the non-is getter with matching type.
     */
    @Test
    fun getter_matchingReturnType_isPreferred() {
        val source = """
        package com.example.objectbox;
        import io.objectbox.annotation.Entity;
        import io.objectbox.annotation.Id;

        @Entity
        public class Example {
            @Id long id;
            
            private Integer isProperty;

            // Should use this one.
            public Integer getIsProperty() {
                return isProperty;
            }
            // Prefer, but skip this one as return type not matching.
            public Boolean isProperty() {
                return isProperty == 1;
            }
        }
        """
        val javaFileObject = JavaFileObjects.forSourceString("com.example.objectbox.Example", source)

        val environment = TestEnvironment("getter-matching-return.json", useTemporaryModelFile = true)

        environment.compile(listOf(javaFileObject))
            .assertThatIt { succeededWithoutWarnings() }

        val entity = environment.schema.entities[0]!!
        val property = entity.properties!!.find { it.propertyName == "isProperty" }!!
        Truth.assertThat(property.getterMethodName).isEqualTo("getIsProperty")
    }

}