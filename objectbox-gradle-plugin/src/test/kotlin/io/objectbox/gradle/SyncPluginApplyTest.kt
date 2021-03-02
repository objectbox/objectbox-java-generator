package io.objectbox.gradle


/**
 * Tests applying [ObjectBoxSyncGradlePlugin] configures project as expected.
 */
class SyncPluginApplyTest : PluginApplyTest() {

    override val pluginId = "io.objectbox.sync"
    override val expectedLibWithSyncVariantPrefix = "objectbox-sync"
    override val expectedLibWithSyncVariantVersion = ProjectEnv.Const.nativeSyncVersionToApply

}