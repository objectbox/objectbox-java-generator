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

package io.objectbox.gradle

import org.gradle.api.Project
import org.gradle.api.artifacts.Dependency
import org.gradle.api.artifacts.DependencySet
import org.gradle.testfixtures.ProjectBuilder
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue


/**
 * Base class to test applying [ObjectBoxGradlePlugin] configures a Gradle project as expected.
 */
abstract class PluginApplyTest {

    open val pluginId = "io.objectbox"
    open val expectedLibWithSyncVariantPrefix = "objectbox"
    open val expectedLibWithSyncVariantVersion = ProjectEnv.Const.nativeVersionToApply

    protected fun buildProject(
        configureProject: Project.() -> Unit
    ): Project = ProjectBuilder
        .builder()
        .build()
        .apply(configureProject)
        .also {
            it.enableObjectBoxPluginDebugMode()
        }

    /**
     * Test PluginOptions extension is created and can be configured.
     * To check if it actually is recognized, would have to assert log output,
     * currently not doing that.
     *
     * This also enables helpful log output to diagnose test failures.
     */
    protected fun Project.enableObjectBoxPluginDebugMode() {
        extensions.apply {
            configure<ObjectBoxPluginExtension>("objectbox") {
                it.debug.set(true)
            }
        }
        assertTrue(extensions.getByType(ObjectBoxPluginExtension::class.java).debug.get())
    }

    /**
     * Gets the dependencies matching the given predicate from all [Project.getConfigurations].
     */
    protected fun Project.getDependenciesMatching(
        predicate: (Dependency) -> Boolean
    ): List<Dependency> =
        configurations
            .flatMap { configuration ->
                configuration.dependencies
                    .filter(predicate)
            }

    fun assertProcessorDependency(apDeps: DependencySet) {
        assertEquals("objectbox-processor dependency not found", 1, apDeps.count {
            it.group == "io.objectbox" && it.name == "objectbox-processor"
                    && it.version == ProjectEnv.Const.pluginVersion
        })
    }

    fun assertNativeDependency(deps: DependencySet) {
        assertEquals("JVM database library dependency not found", 1, deps.count {
            it.group == "io.objectbox"
                    && (it.name == "$expectedLibWithSyncVariantPrefix-linux"
                    || it.name == "$expectedLibWithSyncVariantPrefix-windows"
                    || it.name == "$expectedLibWithSyncVariantPrefix-macos")
                    && it.version == expectedLibWithSyncVariantVersion
        })
    }

}