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

import io.objectbox.gradle.ProjectEnv.Const
import org.gradle.api.artifacts.DependencySet
import org.junit.Assert.assertEquals

fun DependencySet.assertObxDependency(name: String, version: String) {
    assertEquals("$name dependency not found", 1, count {
        it.group == Const.OBX_GROUP && it.name == name
                && it.version == version
    })
}

fun DependencySet.assertJavaDependency() {
    assertObxDependency(Const.OBX_JAVA, Const.OBX_JAVA_VERSION)
}

fun DependencySet.assertKotlinDependency() {
    assertObxDependency(Const.OBX_KOTLIN, Const.OBX_JAVA_VERSION)
}
