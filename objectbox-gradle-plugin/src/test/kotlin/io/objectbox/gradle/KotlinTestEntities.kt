/*
 * ObjectBox Build Tools
 * Copyright (C) 2026 ObjectBox Ltd.
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

import org.junit.rules.TemporaryFolder
import java.io.File

/**
 * The entity classes and expected output of the `kotlin-project` test resources: a set of
 * Kotlin entities (data class with default values, nullable types, is-prefixed property,
 * ToOne/ToMany relations) and a Java entity (accessors, boxed type, public field).
 *
 * The expected output was originally generated with the kapt based setup, the KSP based setup
 * must produce identical output (see KotlinKspCompileTest).
 */
object KotlinTestEntities {

    /**
     * Build script block that compiles Java and Kotlin sources (and kapt stubs) for the same JVM
     * target, as the Kotlin plugin fails the build if they are inconsistent.
     */
    val buildScriptJvmTargetBlock =
        """
        java {
            sourceCompatibility = JavaVersion.VERSION_1_8
            targetCompatibility = JavaVersion.VERSION_1_8
        }
        compileKotlin {
            kotlinOptions.jvmTarget = "1.8"
        }
        """.trimIndent()

    fun addEntitySources(runner: GradleTestRunner) {
        runner.addSourceFile(
            "Customer.kt",
            """
            package com.example

            import io.objectbox.annotation.Entity
            import io.objectbox.annotation.Id

            @Entity
            data class Customer(
                @Id var id: Long = 0,
                var name: String? = null,
                var age: Int = 0,
                var isPremium: Boolean = false,
                var score: Long? = null
            )
            """.trimIndent()
        )
        runner.addSourceFile(
            "Order.kt",
            """
            package com.example

            import io.objectbox.annotation.Entity
            import io.objectbox.annotation.Id
            import io.objectbox.relation.ToMany
            import io.objectbox.relation.ToOne

            @Entity
            class Order {
                @Id var id: Long = 0
                var note: String? = null
                lateinit var customer: ToOne<Customer>
                lateinit var items: ToMany<Item>
            }

            @Entity
            class Item {
                @Id var id: Long = 0
            }
            """.trimIndent()
        )
        // A Java entity in the Kotlin project: Java entities remain supported when mixed with Kotlin.
        runner.addSourceFile(
            "JavaNote.java",
            """
            package com.example;

            import io.objectbox.annotation.Entity;
            import io.objectbox.annotation.Id;

            @Entity
            public class JavaNote {
                @Id private long id;
                private String text;
                private Integer priority;
                public boolean done;

                public long getId() { return id; }
                public void setId(long id) { this.id = id; }
                public String getText() { return text; }
                public void setText(String text) { this.text = text; }
                public Integer getPriority() { return priority; }
                public void setPriority(Integer priority) { this.priority = priority; }
            }
            """.trimIndent()
        )
    }

    /**
     * Copies the expected model file into the project, so all entities and properties resolve to
     * existing UIDs and generated sources are deterministic.
     */
    fun seedModelFile(testProjectDir: TemporaryFolder): File {
        val modelFolder = testProjectDir.newFolder("objectbox-models")
        return File(modelFolder, "default.json").apply {
            writeText(readTestResource("kotlin-project/default.json"))
        }
    }

    fun readTestResource(path: String): String {
        return javaClass.classLoader.getResource(path)?.readText()
            ?: throw IllegalStateException("Test resource not found: $path")
    }

}
