/*
 * Copyright (C) 2022-2023 ObjectBox Ltd.
 *
 * This file is part of ObjectBox Build Tools.
 *
 * ObjectBox Build Tools is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * ObjectBox Build Tools is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with ObjectBox Build Tools.  If not, see <http://www.gnu.org/licenses/>.
 */

package io.objectbox.gradle

import com.google.common.truth.Truth.assertThat
import io.objectbox.gradle.transform.AndroidPlugin72
import io.objectbox.gradle.util.AndroidCompat
import org.gradle.api.Project


/**
 * Tests applying [ObjectBoxSyncGradlePlugin] configures a Java or Kotlin Android Gradle project as expected.
 * Tests with Android Plugin 7.3.
 */
class SyncPluginApplyAndroid73Test : SyncPluginApplyAndroidTest() {

    override fun assertAndroidCompat(project: Project) {
        assertThat(AndroidCompat.getPlugin(project))
            .isInstanceOf(AndroidPlugin72::class.java)
    }

}