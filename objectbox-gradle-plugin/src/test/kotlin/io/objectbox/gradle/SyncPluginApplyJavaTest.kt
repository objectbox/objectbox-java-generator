package io.objectbox.gradle


/**
 * Tests applying [ObjectBoxSyncGradlePlugin] configures a Java or Kotlin desktop Gradle project as expected.
 */
class SyncPluginApplyJavaTest : PluginApplyJavaTest() {

    override val pluginId = "io.objectbox.sync"
    override val expectedLibWithSyncVariantPrefix = "objectbox-sync"
    override val expectedLibWithSyncVariantVersion = ProjectEnv.Const.nativeSyncVersionToApply

}