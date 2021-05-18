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