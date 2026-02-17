/*
 * ObjectBox Build Tools
 * Copyright (C) 2022-2024 ObjectBox Ltd.
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

import org.gradle.api.internal.plugins.PluginApplicationException
import org.gradle.testfixtures.ProjectBuilder
import org.hamcrest.CoreMatchers.instanceOf
import org.hamcrest.MatcherAssert.assertThat
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test


/**
 * Tests applying [ObjectBoxGradlePlugin] with Android Plugin 7.3 fails because it is too old.
 */
class PluginApplyAndroid73Test : PluginApplyTest() {

    @Test
    fun apply_afterAndroidPlugin_failsTooOld() {
        val project = ProjectBuilder.builder().build()
        project.pluginManager.apply("com.android.application")

        assertThrows(PluginApplicationException::class.java) {
            project.project.pluginManager.apply(pluginId)
        }.also {
            assertEquals("Failed to apply plugin '$pluginId'.", it.message)
            assertThat(it.cause, instanceOf(IllegalStateException::class.java))
            assertEquals(it.cause?.message, "The ObjectBox Gradle plugin requires Android Gradle Plugin 8.1 or newer")
        }
    }

}