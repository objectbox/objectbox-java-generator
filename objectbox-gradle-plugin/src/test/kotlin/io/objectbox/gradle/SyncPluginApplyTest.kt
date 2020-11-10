package io.objectbox.gradle

import org.gradle.api.artifacts.DependencySet
import org.gradle.api.artifacts.ModuleDependency
import org.junit.Assert


/**
 * Tests applying [ObjectBoxSyncGradlePlugin] configures project as expected.
 */
class SyncPluginApplyTest : PluginApplyTest() {

    override val pluginId = "io.objectbox.sync"
    private val expectedClassifier = "sync"
    override val expectedNativeLibVersion = ProjectEnv.Const.nativeSyncVersionToApply.removeSuffix(":$expectedClassifier")

    override fun assertNativeDependency(compileDeps: DependencySet) {
        Assert.assertEquals(1, compileDeps.count {
            it.group == "io.objectbox"
                    && (it.name == "objectbox-linux" || it.name == "objectbox-windows" || it.name == "objectbox-macos")
                    && it.version == expectedNativeLibVersion
                    && it is ModuleDependency && it.artifacts.first().classifier == expectedClassifier
        })
    }

    override fun assertAndroidDependency(deps: DependencySet) {
        Assert.assertEquals(1, deps.count { dep ->
            dep.group == "io.objectbox" && dep.name == "objectbox-android"
                    && dep.version == expectedNativeLibVersion
                    && dep is ModuleDependency
                    && dep.artifacts.firstOrNull { it.extension == "aar" && it.classifier == expectedClassifier } != null
        })
    }

}