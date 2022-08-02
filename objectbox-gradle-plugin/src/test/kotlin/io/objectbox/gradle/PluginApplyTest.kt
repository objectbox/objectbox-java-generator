package io.objectbox.gradle

import com.android.build.gradle.AppExtension
import io.objectbox.gradle.transform.ObjectBoxAndroidTransform
import org.gradle.api.Project
import org.gradle.api.artifacts.DependencySet
import org.gradle.api.internal.plugins.PluginApplicationException
import org.gradle.api.internal.project.ProjectInternal
import org.gradle.api.plugins.InvalidPluginException
import org.gradle.testfixtures.ProjectBuilder
import org.hamcrest.CoreMatchers.instanceOf
import org.hamcrest.MatcherAssert.assertThat
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test


/**
 * Tests applying [ObjectBoxGradlePlugin] configures project as expected.
 */
open class PluginApplyTest {

    open val pluginId = "io.objectbox"
    open val expectedLibWithSyncVariantPrefix = "objectbox"
    open val expectedLibWithSyncVariantVersion = ProjectEnv.Const.nativeVersionToApply
    private val expectedNativeLibVersion = ProjectEnv.Const.nativeVersionToApply

    @Test
    fun apply_noRequiredPlugins_fails() {
        val project = ProjectBuilder.builder().build()
        assertThrows(PluginApplicationException::class.java) {
            project.project.pluginManager.apply(pluginId)
        }.also {
            assertEquals("Failed to apply plugin '$pluginId'.", it.message)
            assertThat(it.cause, instanceOf(InvalidPluginException::class.java))
        }
    }

    @Test
    fun apply_beforeJavaPlugin_fails() {
        assertApplyBeforePluginFails("java")
    }

    @Test
    fun apply_beforeApplicationPlugin_fails() {
        assertApplyBeforePluginFails("application")
    }

    @Test
    fun apply_beforeJavaLibraryPlugin_fails() {
        assertApplyBeforePluginFails("java-library")
    }

    private fun assertApplyBeforePluginFails(plugin: String) {
        val project = ProjectBuilder.builder().build()
        assertThrows(PluginApplicationException::class.java) {
            project.pluginManager.apply {
                apply(pluginId)
                apply(plugin)
            }
        }.also {
            assertEquals("Failed to apply plugin '$pluginId'.", it.message)
        }
    }

    /**
     * Test PluginOptions extension is created and can be configured.
     * To check if it actually is recognized, would have to assert log output,
     * currently not doing that.
     */
    private fun Project.enableObjectBoxPluginDebugMode() {
        extensions.apply {
            configure<ObjectBoxPluginExtension>("objectbox") {
                it.debug.set(true)
            }
        }
        assertTrue(extensions.getByType(ObjectBoxPluginExtension::class.java).debug.get())
    }

    @Test
    fun apply_afterJavaPlugin() {
        val project = ProjectBuilder.builder().build()
        project.pluginManager.apply {
            apply("java")
            apply(pluginId)
        }
        project.enableObjectBoxPluginDebugMode()

        assertJavaProject(project, "implementation")
    }

    @Test
    fun apply_afterApplicationPlugin() {
        val project = ProjectBuilder.builder().build()
        project.pluginManager.apply {
            apply("application") // Note: application plugin adds java plugin.
            apply(pluginId)
        }
        project.enableObjectBoxPluginDebugMode()

        assertJavaProject(project, "implementation")
    }

    @Test
    fun apply_afterJavaLibraryPlugin() {
        val project = ProjectBuilder.builder().build()
        project.pluginManager.apply {
            apply("java-library")
            apply(pluginId)
        }
        project.enableObjectBoxPluginDebugMode()

        assertJavaProject(project, "api")
    }

    private fun assertJavaProject(project: Project, configuration: String) {
        with(project.configurations) {
            assertProcessorDependency(getByName("annotationProcessor").dependencies)

            getByName(configuration).dependencies.let {
                assertJavaDependency(it)
                assertNativeDependency(it)
            }
        }
        assertNotNull(project.tasks.findByPath("objectboxPrepareBuild"))

        // Note: using the internal evaluate is not nice, but beats writing a full-blown integration test.
        (project as ProjectInternal).evaluate()

        // AFTER EVALUATE.
        // Note: by default only main and test source sets exist.
        assertTransformTask(project, "", "classes")
        assertTransformTask(project, "Test", "testClasses")
    }

    private fun assertTransformTask(
        project: Project,
        sourceSetSuffix: String,
        classesTaskName: String
    ) {
        // Is created.
        val transformTask = project.tasks.findByPath("transform${sourceSetSuffix}ObjectBoxClasses")
        assertNotNull(transformTask)

        // Depends on compile task of source set.
        assertEquals(1, transformTask!!
            .taskDependencies.getDependencies(transformTask).count { it.name == "compile${sourceSetSuffix}Java" })

        // Classes task of source set should depend on it.
        val classesTask = project.tasks.getByName(classesTaskName)
        assertEquals(1, classesTask
            .taskDependencies.getDependencies(classesTask).count { it.name == transformTask.name })
    }

    @Test
    fun apply_afterKotlinAndKaptPlugin() {
        val project = ProjectBuilder.builder().build()
        project.pluginManager.apply {
            apply("kotlin")
            apply("kotlin-kapt")
            apply(pluginId)
        }
        project.enableObjectBoxPluginDebugMode()

        assertKotlinSetup(project)
    }

    @Test
    fun apply_afterKotlinPlugin_addsKapt() {
        val project = ProjectBuilder.builder().build()
        project.pluginManager.apply {
            apply("kotlin")
            apply(pluginId)
        }
        project.enableObjectBoxPluginDebugMode()

        assertKotlinSetup(project)
    }

    private fun assertKotlinSetup(project: Project) {
        with(project.configurations) {
            assertProcessorDependency(getByName("kapt").dependencies)

            getByName("api").dependencies.let { deps ->
                assertEquals(1, deps.count {
                    it.group == "io.objectbox" && it.name == "objectbox-kotlin"
                            && it.version == ProjectEnv.Const.javaVersionToApply
                })
                assertJavaDependency(deps)
                assertNativeDependency(deps)
            }
        }
        assertNotNull(project.tasks.findByPath("objectboxPrepareBuild"))

        // Note: using the internal evaluate is not nice, but beats writing a full-blown integration test.
        (project as ProjectInternal).evaluate()

        // AFTER EVALUATE.
        // Note: by default only main and test source sets exist.
        // Note: transform is not supported for Kotlin code/tasks, so these match plain Java plugin.
        assertTransformTask(project, "", "classes")
        assertTransformTask(project, "Test", "testClasses")
    }

    @Test
    fun apply_afterAndroidPlugin() {
        val project = ProjectBuilder.builder().build()
        project.pluginManager.apply {
            apply("com.android.application")
            apply(pluginId)
        }
        project.enableObjectBoxPluginDebugMode()

        with(project.configurations) {
            assertProcessorDependency(getByName("annotationProcessor").dependencies)
            assertAndroidDependency(getByName("api").dependencies)
            assertNativeDependency(getByName("testImplementation").dependencies)
        }
        assertNotNull(project.tasks.findByPath("objectboxPrepareBuild"))

        // Special for Android: has byte-code transform.
        assertAndroidByteCodeTransform(project)

        // Note: can not evaluate and assert transform task for unit tests as Android plugin requires actual project.
    }

    @Test
    fun apply_afterKotlinAndroidAndKaptPlugin() {
        val project = ProjectBuilder.builder().build()
        project.pluginManager.apply {
            apply("com.android.application")
            apply("kotlin-android")
            apply("kotlin-kapt")
            apply(pluginId)
        }
        project.enableObjectBoxPluginDebugMode()

        assertKotlinAndroidSetup(project)
    }

    @Test
    fun apply_afterKotlinAndroidPlugin_addsKapt() {
        val project = ProjectBuilder.builder().build()
        project.pluginManager.apply {
            apply("com.android.application")
            apply("kotlin-android")
            apply(pluginId)
        }
        project.enableObjectBoxPluginDebugMode()

        assertKotlinAndroidSetup(project)
    }

    private fun assertKotlinAndroidSetup(project: Project) {
        with(project.configurations) {
            assertProcessorDependency(getByName("kapt").dependencies)
            assertAndroidDependency(getByName("api").dependencies)
            assertNativeDependency(getByName("testImplementation").dependencies)
        }
        assertNotNull(project.tasks.findByPath("objectboxPrepareBuild"))

        // Special for Android: has byte-code transform.
        assertAndroidByteCodeTransform(project)
    }

    private fun assertProcessorDependency(apDeps: DependencySet) {
        assertEquals(1, apDeps.count {
            it.group == "io.objectbox" && it.name == "objectbox-processor"
                    && it.version == ProjectEnv.Const.pluginVersion
        })
    }

    private fun assertJavaDependency(compileDeps: DependencySet) {
        assertEquals(1, compileDeps.count {
            it.group == "io.objectbox" && it.name == "objectbox-java"
                    && it.version == ProjectEnv.Const.javaVersionToApply
        })
    }

    open fun assertNativeDependency(compileDeps: DependencySet) {
        // Note: there are no Sync variants for Windows and Mac, yet.
        assertEquals(1, compileDeps.count {
            it.group == "io.objectbox"
                    && ((it.name == "$expectedLibWithSyncVariantPrefix-linux" && it.version == expectedLibWithSyncVariantVersion)
                    || ((it.name == "objectbox-windows" || it.name == "objectbox-macos") && it.version == expectedNativeLibVersion))
        })
    }

    open fun assertAndroidDependency(deps: DependencySet) {
        assertEquals(1, deps.count {
            it.group == "io.objectbox" && it.name == "$expectedLibWithSyncVariantPrefix-android"
                    && it.version == expectedLibWithSyncVariantVersion
        })
    }

    private fun assertAndroidByteCodeTransform(project: Project) {
        with(project.extensions.getByType(AppExtension::class.java)) {
            assertEquals(1, transforms.count { it is ObjectBoxAndroidTransform })
        }
    }
}