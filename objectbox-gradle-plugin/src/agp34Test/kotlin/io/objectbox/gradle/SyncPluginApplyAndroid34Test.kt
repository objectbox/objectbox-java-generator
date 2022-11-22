package io.objectbox.gradle

import com.google.common.truth.Truth.assertThat
import io.objectbox.gradle.transform.AndroidPlugin34
import io.objectbox.gradle.util.AndroidCompat
import org.gradle.api.Project


/**
 * Tests applying [ObjectBoxSyncGradlePlugin] configures a Java or Kotlin Android Gradle project as expected.
 * Tests with Android Plugin 3.4.
 */
class SyncPluginApplyAndroid34Test : SyncPluginApplyAndroidTest() {

    override fun assertAndroidCompat(project: Project) {
        assertThat(AndroidCompat.getPlugin(project))
            .isInstanceOf(AndroidPlugin34::class.java)
    }

}