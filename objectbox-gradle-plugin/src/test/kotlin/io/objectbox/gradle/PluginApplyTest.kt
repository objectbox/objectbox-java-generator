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
import org.junit.Assert.assertTrue


/**
 * Base class to test applying [ObjectBoxGradlePlugin] configures a Gradle project as expected.
 */
abstract class PluginApplyTest {

    open val pluginId = "io.objectbox"
    open val expectedLibWithSyncVariantPrefix = "objectbox"
    open val expectedLibWithSyncVariantVersion = ProjectEnv.Const.nativeVersionToApply
    val expectedNativeLibVersion = ProjectEnv.Const.nativeVersionToApply

    /**
     * Test PluginOptions extension is created and can be configured.
     * To check if it actually is recognized, would have to assert log output,
     * currently not doing that.
     */
    protected fun Project.enableObjectBoxPluginDebugMode() {
        extensions.apply {
            configure<ObjectBoxPluginExtension>("objectbox") {
                it.debug.set(true)
            }
        }
        assertTrue(extensions.getByType(ObjectBoxPluginExtension::class.java).debug.get())
    }
}