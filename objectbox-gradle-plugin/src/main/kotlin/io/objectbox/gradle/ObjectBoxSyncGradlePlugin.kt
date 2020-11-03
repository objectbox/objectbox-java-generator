package io.objectbox.gradle


/**
 * Like [ObjectBoxGradlePlugin], but adds native libraries that are sync-enabled as dependencies.
 */
class ObjectBoxSyncGradlePlugin : ObjectBoxGradlePlugin() {

    override val pluginId = "io.objectbox.sync"

    override fun getNativeLibraryVersionToApply(): String {
        return ProjectEnv.Const.nativeSyncVersionToApply
    }

}