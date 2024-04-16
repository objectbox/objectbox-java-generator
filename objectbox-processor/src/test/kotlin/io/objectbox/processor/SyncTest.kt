/*
 * ObjectBox Build Tools
 * Copyright (C) 2020-2024 ObjectBox Ltd.
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
import com.google.testing.compile.JavaFileObjects
import io.objectbox.model.EntityFlags
import org.junit.Test

/**
 * Tests related to the @Sync annotation.
 */
class SyncTest : BaseProcessorTest() {

    @Test
    fun sync_works() {
        val sourceFile = """
        package com.example;
        
        import io.objectbox.annotation.ConflictStrategy;
        import io.objectbox.annotation.Entity;
        import io.objectbox.annotation.Id;
        import io.objectbox.annotation.Sync;
        import io.objectbox.annotation.Unique;

        @Entity
        @Sync
        public class Example {
            @Id long id;
            
            @Unique(onConflict = ConflictStrategy.REPLACE)
            public long replaceProp;
        }
        """.trimIndent().let {
            JavaFileObjects.forSourceString("com.example.Example", it)
        }

        // Need stable model file + ids to verify sources match.
        TestEnvironment("sync-works.json").compile(listOf(sourceFile))
            .assertThatIt { succeededWithoutWarnings() }
            // Entity flag added to generated code
            .assertGeneratedSourceMatches("com.example.MyObjectBox", "MyObjectBox-sync.java")

        // Use temp model file to assert model file.
        TestEnvironment("sync-works.json", useTemporaryModelFile = true).let { environment ->
            environment.compile(listOf(sourceFile))
                .assertThatIt { succeededWithoutWarnings() }

            // Schema matches
            environment.schema.entities[0].let {
                assertThat(it.isSyncEnabled).isTrue()
            }

            // Model file has sync enabled flag
            environment.readModel().findEntity("Example", null)!!.let {
                assertThat(it.flags).isNotNull()
                // Note: model file should not contain EntityFlags.USE_NO_ARG_CONSTRUCTOR.
                assertThat(it.flags).isEqualTo(EntityFlags.SYNC_ENABLED)
            }
        }
    }

    @Test
    fun sync_relationToSyncedEntity_works() {
        val exampleFile = """
        package com.example;
        import io.objectbox.annotation.Entity;
        import io.objectbox.annotation.Id;
        import io.objectbox.annotation.Sync;
        import io.objectbox.relation.ToMany;

        @Entity
        @Sync
        public class Example {
            @Id long id;
            ToMany<Related> relation;
        }
        """.trimIndent().let {
            JavaFileObjects.forSourceString("com.example.Example", it)
        }
        val relatedFile = """
        package com.example;
        import io.objectbox.annotation.Entity;
        import io.objectbox.annotation.Id;
        import io.objectbox.annotation.Sync;

        @Entity
        @Sync
        public class Related {
            @Id long id;
        }
        """.trimIndent().let {
            JavaFileObjects.forSourceString("com.example.Related", it)
        }

        val environment = TestEnvironment("sync-relation-works.json", useTemporaryModelFile = true)

        environment.compile(listOf(exampleFile, relatedFile))
            .assertThatIt { succeededWithoutWarnings() }
    }

    @Test
    fun sync_relationToNotSyncedEntity_fails() {
        val exampleFile = """
        package com.example;
        import io.objectbox.annotation.Entity;
        import io.objectbox.annotation.Id;
        import io.objectbox.annotation.Sync;
        import io.objectbox.relation.ToMany;

        @Entity
        @Sync
        public class Example {
            @Id long id;
            ToMany<Related> relation;
        }
        """.trimIndent().let {
            JavaFileObjects.forSourceString("com.example.Example", it)
        }
        val relatedFile = """
        package com.example;
        import io.objectbox.annotation.Entity;
        import io.objectbox.annotation.Id;

        @Entity
        public class Related {
            @Id long id;
        }
        """.trimIndent().let {
            JavaFileObjects.forSourceString("com.example.Related", it)
        }

        val environment = TestEnvironment("sync-relation-fails.json", useTemporaryModelFile = true)

        environment.compile(listOf(exampleFile, relatedFile))
            .assertThatIt {
                failed()
                hadErrorContaining(
                    "Synced entity 'Example' can't have a relation to not-synced entity 'Related', but found relation 'relation'."
                )
            }
        assertThat(environment.isModelFileExists()).isFalse()
    }

    @Test
    fun sync_sharedGlobalIds_works() {
        val sourceFile = """
        package com.example;
        import io.objectbox.annotation.Entity;
        import io.objectbox.annotation.Id;
        import io.objectbox.annotation.Sync;

        @Entity
        @Sync(sharedGlobalIds = true)
        public class Example {
            @Id long id;
        }
        """.trimIndent().let {
            JavaFileObjects.forSourceString("com.example.Example", it)
        }

        // Need stable model file + ids to verify sources match.
        TestEnvironment("sync-global-ids-works.json")
            .compile(listOf(sourceFile))
            .assertThatIt { succeededWithoutWarnings() }
            // Entity flag added to generated code
            .assertGeneratedSourceMatches("com.example.MyObjectBox", "MyObjectBox-sync-global-ids.java")

        // Use temp model file to assert model file.
        TestEnvironment("sync-global-ids-works.json", useTemporaryModelFile = true).let { environment ->
            environment.compile(listOf(sourceFile))
                .assertThatIt { succeededWithoutWarnings() }

            // Schema matches
            environment.schema.entities[0].let {
                assertThat(it.isSyncSharedGlobalIds).isTrue()
                assertThat(it.isSyncEnabled).isTrue()
            }

            // Model file has global IDs flag
            environment.readModel().findEntity("Example", null)!!.let {
                assertThat(it.flags).isNotNull()
                // Note: model file should not contain EntityFlags.USE_NO_ARG_CONSTRUCTOR.
                assertThat(it.flags).isEqualTo(EntityFlags.SHARED_GLOBAL_IDS or EntityFlags.SYNC_ENABLED)
            }
        }
    }

    @Test
    fun sync_uniqueNotReplace_fails() {
        val exampleFile = """
        package com.example;
        
        import io.objectbox.annotation.ConflictStrategy;
        import io.objectbox.annotation.Entity;
        import io.objectbox.annotation.Id;
        import io.objectbox.annotation.Sync;
        import io.objectbox.annotation.Unique;

        @Entity
        @Sync
        public class Example {
            @Id long id;
            
            @Unique(onConflict = ConflictStrategy.REPLACE)
            public long replaceProp;
            
            @Unique
            public long failProp;
        }
        """.trimIndent().let {
            JavaFileObjects.forSourceString("com.example.Example", it)
        }

        val environment = TestEnvironment("not-generated.json", useTemporaryModelFile = true)

        environment.compile(listOf(exampleFile))
            .assertThatIt {
                failed()
                hadErrorContaining(
                    "Synced entities must use @Unique(onConflict = ConflictStrategy.REPLACE) for all unique properties"
                )
            }
        assertThat(environment.isModelFileExists()).isFalse()
    }
}