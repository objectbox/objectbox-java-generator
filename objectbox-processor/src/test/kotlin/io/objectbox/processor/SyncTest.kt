package io.objectbox.processor

import com.google.common.truth.Truth.assertThat
import com.google.testing.compile.CompilationSubject
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
        import io.objectbox.annotation.Entity;
        import io.objectbox.annotation.Id;
        import io.objectbox.annotation.Sync;

        @Entity
        @Sync
        public class Example {
            @Id long id;
        }
        """.trimIndent().let {
            JavaFileObjects.forSourceString("com.example.Example", it)
        }

        // Need stable model file + ids to verify sources match.
        TestEnvironment("sync-works.json").let {
            val compilation = it.compile(listOf(sourceFile))
            CompilationSubject.assertThat(compilation).succeededWithoutWarnings()

            // Entity flag added to generated code
            compilation.assertGeneratedSourceMatches("com.example.MyObjectBox", "MyObjectBox-sync.java")
        }

        // Use temp model file to assert model file.
        TestEnvironment("sync-works-temp.json").let { environment ->
            environment.cleanModelFile()

            val compilation = environment.compile(listOf(sourceFile))
            CompilationSubject.assertThat(compilation).succeededWithoutWarnings()

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

        val environment = TestEnvironment("sync-relation-works-temp.json")
        environment.cleanModelFile()

        val compilation = environment.compile(listOf(exampleFile, relatedFile))
        CompilationSubject.assertThat(compilation).succeededWithoutWarnings()
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

        val environment = TestEnvironment("sync-relation-fails-temp.json")
        environment.cleanModelFile()

        val compilation = environment.compile(listOf(exampleFile, relatedFile))
        CompilationSubject.assertThat(compilation).failed()
        CompilationSubject.assertThat(compilation).hadErrorContaining(
            "Synced entity 'Example' can't have a relation to not-synced entity 'Related'."
        )
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
        TestEnvironment("sync-global-ids-works.json").let {
            val compilation = it.compile(listOf(sourceFile))
            CompilationSubject.assertThat(compilation).succeededWithoutWarnings()

            // Entity flag added to generated code
            compilation.assertGeneratedSourceMatches("com.example.MyObjectBox", "MyObjectBox-sync-global-ids.java")
        }

        // Use temp model file to assert model file.
        TestEnvironment("sync-global-ids-works-temp.json").let { environment ->
            environment.cleanModelFile()

            val compilation = environment.compile(listOf(sourceFile))
            CompilationSubject.assertThat(compilation).succeededWithoutWarnings()

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
}