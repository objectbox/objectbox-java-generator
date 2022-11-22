package io.objectbox.gradle

/**
 * Base class to test applying [ObjectBoxSyncGradlePlugin] configures a Java or Kotlin Android Gradle project as expected.
 */
abstract class SyncPluginApplyAndroidTest : PluginApplyAndroidTest() {

    override val pluginId = "io.objectbox.sync"
    override val expectedLibWithSyncVariantPrefix = "objectbox-sync"
    override val expectedLibWithSyncVariantVersion = ProjectEnv.Const.nativeSyncVersionToApply

}