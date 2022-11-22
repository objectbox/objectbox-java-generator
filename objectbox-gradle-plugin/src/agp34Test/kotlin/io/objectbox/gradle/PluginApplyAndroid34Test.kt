package io.objectbox.gradle

import com.google.common.truth.Truth.assertThat
import io.objectbox.gradle.transform.AndroidPlugin33
import io.objectbox.gradle.util.AndroidCompat
import org.gradle.api.Project


/**
 * Tests applying [ObjectBoxGradlePlugin] configures a Java or Kotlin Android Gradle project as expected.
 * Tests with Android Plugin 3.4.
 */
class PluginApplyAndroid34Test : PluginApplyAndroidTest() {

    override fun assertAndroidCompat(project: Project) {
        assertThat(AndroidCompat.getPlugin(project))
            .isInstanceOf(AndroidPlugin33::class.java)
    }

}