package io.objectbox.gradle

import org.gradle.api.Project
import org.junit.Assert.assertTrue


/**
 * Base class to test applying [ObjectBoxGradlePlugin] configures a Gradle project as expected.
 */
abstract class PluginApplyTest {

    open val pluginId = "io.objectbox"
    open val expectedLibWithSyncVariantPrefix = "objectbox"
    open val expectedLibWithSyncVariantVersion = ProjectEnv.Const.nativeVersionToApply
    val expectedNativeLibVersion = ProjectEnv.Const.nativeVersionToApply

    /**
     * Test PluginOptions extension is created and can be configured.
     * To check if it actually is recognized, would have to assert log output,
     * currently not doing that.
     */
    protected fun Project.enableObjectBoxPluginDebugMode() {
        extensions.apply {
            configure<ObjectBoxPluginExtension>("objectbox") {
                it.debug.set(true)
            }
        }
        assertTrue(extensions.getByType(ObjectBoxPluginExtension::class.java).debug.get())
    }
}