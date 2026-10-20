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

import com.google.common.truth.Truth.assertThat
import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.TaskOutcome
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

/**
 * Regression test for https://github.com/objectbox/objectbox-java/issues/1114: a manual edit to
 * `objectbox-models/default.json` (e.g. to resolve an id conflict) must invalidate the Gradle build cache entry for
 * the compile/annotation-processing task, even though the file isn't a source file and no Java/Kotlin source
 * changed.
 *
 * Like other `GradleTestRunner`-based tests, this builds a real project and so needs GitLab credentials (see
 * AGENTS.md) to resolve the `objectbox-java` snapshot dependency, or `OBX_RELEASE=true` plus a placeholder
 * `-PgitlabUrl`/`-PgitlabToken` (any https URL; never actually contacted) to resolve the released version from
 * Maven Central instead.
 */
class BuildCacheTest {

    @JvmField
    @Rule
    val testProjectDir: TemporaryFolder = TemporaryFolder.builder().assureDeletion().build()

    private val exampleEntity =
        """
        package com.example;

        import io.objectbox.annotation.Entity;
        import io.objectbox.annotation.Id;

        @Entity
        public class Example {
            @Id public long id;
        }
        """.trimIndent()

    @Test
    fun modelFileChange_invalidatesBuildCache_forJavaAnnotationProcessing() {
        // A dedicated, persistent TestKit Gradle user home so the local build cache populated by one build() call
        // is actually seen by the next: GradleTestRunner.build() creates a new GradleRunner per call, which
        // otherwise defaults to an isolated home per run.
        val testKitDir = testProjectDir.newFolder("testkit-home")

        val gradleRunner = GradleTestRunner(testProjectDir)
            .apply { additionalPlugins += "java-library" }
        gradleRunner.addSourceFile("Example.java", exampleEntity)

        // 1) First-ever build: the model file does not exist yet when the cache key for ':compileJava' is
        // computed, so its input fingerprint differs from all later builds where the file exists. Just a warm-up.
        gradleRunner.buildWithCache(testKitDir)

        val modelFile = File(testProjectDir.root, "objectbox-models/default.json")
        assertThat(modelFile.exists()).isTrue()

        // 2) Second warm-up: the model file now exists and is unchanged, so this establishes the cache entry that
        // step 3 is expected to reuse.
        deleteBuildOutput()
        gradleRunner.buildWithCache(testKitDir)

        // 3) Simulate a fresh checkout (no local task history) with nothing changed since step 2: must be a cache
        // hit. This confirms the setup actually exercises the build cache, not just up-to-date checking.
        deleteBuildOutput()
        val unchangedRebuild = gradleRunner.buildWithCache(testKitDir)
        assertThat(unchangedRebuild.task(":compileJava")!!.outcome).isEqualTo(TaskOutcome.FROM_CACHE)

        // 4) Manually edit the model file, e.g. as when resolving a conflict, without touching any source file.
        modelFile.appendText("\n")

        // 5) Simulate a fresh checkout again: the edited model file must cause a real rebuild, not a stale
        // cache hit that would silently keep the previous (potentially conflicting) ids around.
        deleteBuildOutput()
        val rebuildAfterModelEdit = gradleRunner.buildWithCache(testKitDir)
        assertThat(rebuildAfterModelEdit.task(":compileJava")!!.outcome).isEqualTo(TaskOutcome.SUCCESS)
    }

    private fun GradleTestRunner.buildWithCache(testKitDir: File): BuildResult =
        build(listOf("--build-cache", "assemble")) { it.withTestKitDir(testKitDir) }

    private fun deleteBuildOutput() {
        File(testProjectDir.root, "build").deleteRecursively()
    }

}
