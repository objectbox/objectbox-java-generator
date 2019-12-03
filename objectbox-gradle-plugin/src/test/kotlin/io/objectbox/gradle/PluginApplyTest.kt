package io.objectbox.gradle

import com.android.build.gradle.AppExtension
import io.objectbox.gradle.transform.ObjectBoxAndroidTransform
import org.gradle.api.Project
import org.gradle.api.artifacts.DependencySet
import org.gradle.api.internal.plugins.PluginApplicationException
import org.gradle.api.internal.project.ProjectInternal
import org.gradle.api.plugins.InvalidPluginException
import org.gradle.testfixtures.ProjectBuilder
import org.hamcrest.CoreMatchers.isA
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Rule
import org.junit.Test
import org.junit.rules.ExpectedException


/**
 * Tests applying plugin configures project as expected.
 */
class PluginApplyTest {

    @Rule
    @JvmField
    val exception: ExpectedException = ExpectedException.none()

    @Test
    fun apply_noRequiredPlugins_fails() {
        exception.expect(PluginApplicationException::class.java)
        exception.expectMessage("Failed to apply plugin [id 'io.objectbox']")
        exception.expectCause(isA(InvalidPluginException::class.java))

        val project = ProjectBuilder.builder().build()
        project.project.pluginManager.apply("io.objectbox")
    }

    @Test
    fun apply_beforeJavaPlugin_fails() {
        exception.expect(PluginApplicationException::class.java)
        exception.expectMessage("Failed to apply plugin [id 'io.objectbox']")

        val project = ProjectBuilder.builder().build()
        project.pluginManager.apply {
            apply("io.objectbox")
            apply("java")
        }
    }

    @Test
    fun apply_afterJavaPlugin() {
        val project = ProjectBuilder.builder().build()
        project.pluginManager.apply {
            apply("java")
            apply("io.objectbox")
        }

        with(project.configurations) {
            assertProcessorDependency(getByName("annotationProcessor").dependencies)

            getByName("compile").dependencies.let {
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
        // Created.
        val transformTask = project.tasks.findByPath("objectboxJavaTransform$sourceSetSuffix")
        assertNotNull(transformTask)

        // Must run after compile task.
        assertEquals(1, transformTask!!
            .mustRunAfter.getDependencies(transformTask).count { it.name == "compile${sourceSetSuffix}Java" })

        // Classes task should depend on it.
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
            apply("io.objectbox")
        }

        assertKotlinSetup(project)
    }

    @Test
    fun apply_afterKotlinPlugin_addsKapt() {
        val project = ProjectBuilder.builder().build()
        project.pluginManager.apply {
            apply("kotlin")
            apply("io.objectbox")
        }

        assertKotlinSetup(project)
    }

    private fun assertKotlinSetup(project: Project) {
        with(project.configurations) {
            assertProcessorDependency(getByName("kapt").dependencies)

            getByName("api").dependencies.let { deps ->
                assertEquals(1, deps.count {
                    it.group == "io.objectbox" && it.name == "objectbox-kotlin"
                            && it.version == ProjectEnv.Const.runtimeVersion
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
            apply("io.objectbox")
        }

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
            apply("io.objectbox")
        }

        assertKotlinAndroidSetup(project)
    }

    @Test
    fun apply_afterKotlinAndroidPlugin_addsKapt() {
        val project = ProjectBuilder.builder().build()
        project.pluginManager.apply {
            apply("com.android.application")
            apply("kotlin-android")
            apply("io.objectbox")
        }

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
                    && it.version == ProjectEnv.Const.runtimeVersion
        })
    }

    private fun assertNativeDependency(compileDeps: DependencySet) {
        assertEquals(1, compileDeps.count {
            it.group == "io.objectbox"
                    && (it.name == "objectbox-linux" || it.name == "objectbox-windows" || it.name == "objectbox-macos")
                    && it.version == ProjectEnv.Const.runtimeVersion
        })
    }

    private fun assertAndroidDependency(deps: DependencySet) {
        assertEquals(1, deps.count {
            it.group == "io.objectbox" && it.name == "objectbox-android"
                    && it.version == ProjectEnv.Const.runtimeVersion
        })
    }

    private fun assertAndroidByteCodeTransform(project: Project) {
        with(project.extensions.getByType(AppExtension::class.java)) {
            assertEquals(1, transforms.count { it is ObjectBoxAndroidTransform })
        }
    }
}