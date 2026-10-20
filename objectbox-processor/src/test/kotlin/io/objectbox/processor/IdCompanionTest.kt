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
import io.objectbox.model.PropertyFlags
import org.junit.Test

/**
 * Additional tests for the IdCompanion annotation. For basic tests see SimpleEntityTest.
 */
class IdCompanionTest(processorType: ProcessorType) : BaseProcessorTest(processorType) {

    @Test
    fun datenano_primitive_supported() {
        val dateNanoLongPrimitive = """
        package com.example;
        import io.objectbox.annotation.DatabaseType;
        import io.objectbox.annotation.Entity;
        import io.objectbox.annotation.Id;
        import io.objectbox.annotation.IdCompanion;
        import io.objectbox.annotation.Type;       

        @Entity
        public class Example {
            @Id long id;
            @IdCompanion @Type(DatabaseType.DateNano) long companion;
        }
        """.trimIndent()

        assertIdCompanionDateNano(dateNanoLongPrimitive)
    }

    @Test
    fun datenano_nullable_supported() {
        val dateNanoLongNullable = """
        package com.example;
        import io.objectbox.annotation.DatabaseType;
        import io.objectbox.annotation.Entity;
        import io.objectbox.annotation.Id;
        import io.objectbox.annotation.IdCompanion;
        import io.objectbox.annotation.Type;

        @Entity
        public class Example {
            @Id long id;
            @IdCompanion @Type(DatabaseType.DateNano) Long companion;
        }
        """.trimIndent()

        assertIdCompanionDateNano(dateNanoLongNullable)
    }

    private fun assertIdCompanionDateNano(exampleSource: String) {
        val environment = env("idcompanion-datenano.json", useTemporaryModelFile = true)

        environment.addSourceFile("com.example.Example", exampleSource)
        environment.compile()
            .assertThatIt { succeededWithoutWarnings() }

        // Assert model file.
        val model = environment.readModel()
        val modelEntity = model.findEntity("Example", null)
        assertThat(modelEntity).isNotNull()
        val modelPropertyCompanion = modelEntity!!.properties.find { it.name == "companion" }
        assertThat(modelPropertyCompanion).isNotNull()
        assertThat(modelPropertyCompanion!!.type).isEqualTo(io.objectbox.model.PropertyType.DateNano)
        assertThat(modelPropertyCompanion.flags).isEqualTo(PropertyFlags.ID_COMPANION)
    }

    @Test
    fun multiple_error() {
        val sourceFile = """
        package com.example;
        import io.objectbox.annotation.Entity;
        import io.objectbox.annotation.Id;
        import io.objectbox.annotation.IdCompanion;
        import java.util.Date;

        @Entity
        public class Example {
            @Id long id;
            @IdCompanion Date companion1;
            @IdCompanion Date companion2;
        }
        """.trimIndent()

        val environment = env("idcompanion-multiple.json", useTemporaryModelFile = true)

        environment.addSourceFile("com.example.Example", sourceFile)
        environment.compile()
            .assertThatIt {
                hadErrorContaining("'companion1' is already an @IdCompanion property, there can only be one.")
            }
    }

    @Test
    fun not_date_unsupported() {
        val sourceFile = """
        package com.example;
        import io.objectbox.annotation.Entity;
        import io.objectbox.annotation.Id;
        import io.objectbox.annotation.IdCompanion;

        @Entity
        public class Example {
            @Id long id;
            @IdCompanion Long companion;
        }
        """.trimIndent()

        val environment = env("idcompanion-not-date.json", useTemporaryModelFile = true)

        environment.addSourceFile("com.example.Example", sourceFile)
        environment.compile()
            .assertThatIt {
                hadErrorContaining("@IdCompanion has to be of type Date or a long annotated with @Type(DateNano).")
            }
    }

}