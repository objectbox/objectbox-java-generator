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

package io.objectbox.gradle


/**
 * Like [ObjectBoxGradlePlugin], but adds native libraries that are sync-enabled as dependencies.
 */
class ObjectBoxSyncGradlePlugin : ObjectBoxGradlePlugin() {

    override val pluginId = "io.objectbox.sync"

    override fun getLibWithSyncVariantPrefix(): String {
        // Use Sync version.
        return LIBRARY_NAME_PREFIX_SYNC
    }

    override fun getLibWithSyncVariantVersion(): String {
        return ProjectEnv.Const.nativeSyncVersionToApply
    }

}