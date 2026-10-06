/*
 * ObjectBox Build Tools
 * Copyright (C) 2022-2026 ObjectBox Ltd.
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


/**
 * Like [PluginApplyAndroid810Test], but tests applying [ObjectBoxSyncGradlePlugin].
 */
class SyncPluginApplyAndroid810Test : PluginApplyAndroid810Test() {

    override val pluginId = Const.SYNC_PLUGIN_ID
    override val expectedLibWithSyncVariantPrefix = "objectbox-sync"
    override val expectedLibWithSyncVariantVersion = Const.OBX_DATABASE_SYNC_VERSION

}
