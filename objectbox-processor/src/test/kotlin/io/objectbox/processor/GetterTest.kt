/*
 * ObjectBox Build Tools
 * Copyright (C) 2019-2024 ObjectBox Ltd.
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

package io.objectbox.processor

import com.google.common.truth.Truth.assertThat
import org.intellij.lang.annotations.Language
import org.junit.Test

/** Tests related to detecting getter methods of properties. */
class GetterTest(processorType: ProcessorType) : BaseProcessorTest(processorType) {

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
        val environment = env("getter-is.json", useTemporaryModelFile = true)

        environment.addSourceFile("com.example.objectbox.Example", source)
        environment.compile()
            .assertThatIt { succeededWithoutWarnings() }

        val entity = environment.schema.entities[0]!!
        val property = entity.properties!!.find { it.propertyName == "isProperty" }!!
        assertThat(property.getterMethodName).isEqualTo("isProperty")
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
        val environment = env("getter-matching-return.json", useTemporaryModelFile = true)

        environment.addSourceFile("com.example.objectbox.Example", source)
        environment.compile()
            .assertThatIt { succeededWithoutWarnings() }

        val entity = environment.schema.entities[0]!!
        val property = entity.properties!!.find { it.propertyName == "isProperty" }!!
        assertThat(property.getterMethodName).isEqualTo("getIsProperty")
    }

    /**
     * A Kotlin property getter renamed with `@JvmName` (on the property with the `get` use-site
     * target, or on the getter itself) is called by its new name. The renamed name also takes
     * precedence over the "is" prefix rule.
     */
    @Test
    fun getter_kotlinJvmName_isUsed() {
        assumeKotlinSourcesSupported()

        @Language("kotlin")
        val source = """
        package com.example.objectbox

        import io.objectbox.annotation.Entity
        import io.objectbox.annotation.Id

        @Entity
        class Example(
            @Id var id: Long = 0,
            @get:JvmName("readName")
            var name: String? = null,
            @get:JvmName("hasFlag")
            var isFlag: Boolean = false
        ) {
            var count: Int = 0
                @JvmName("readCount") get
        }
        """.trimIndent()
        val environment = env("getter-jvm-name.json", useTemporaryModelFile = true)

        environment.addKotlinSourceFile("com.example.objectbox.Example", source)
        // The generated Cursor calls the getters, so it only compiles if the renamed names are used
        environment.compile()
            .assertThatIt { succeededWithoutWarnings() }

        val getterNames = environment.schema.entities.single().properties
            .associate { it.propertyName to it.getterMethodName }
        assertThat(getterNames).containsAtLeast(
            "name", "readName",
            "isFlag", "hasFlag",
            "count", "readCount"
        )
    }

}