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


/**
 * Like [Android9ProjectPluginTest], but tests with the latest version of Gradle. Notably, this build itself can not
 * use that version as it still runs tests with the Android Plugin 8 series, which Gradle 9.6 can no longer run
 * (see the root build script).
 */
class Android9LatestGradleProjectPluginTest : Android9ProjectPluginTest() {

    override val gradleVersion = "9.6.1"

}
