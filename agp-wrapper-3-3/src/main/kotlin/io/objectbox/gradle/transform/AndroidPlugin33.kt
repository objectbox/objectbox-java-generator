package io.objectbox.gradle.transform

import com.android.build.api.transform.Transform
import com.android.build.gradle.AppExtension
import com.android.build.gradle.BaseExtension
import com.android.build.gradle.FeatureExtension
import com.android.build.gradle.LibraryExtension
import com.android.build.gradle.api.BaseVariant
import com.android.build.gradle.api.UnitTestVariant
import org.gradle.api.Project
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.provider.Property
import org.gradle.api.tasks.testing.Test
import org.jetbrains.kotlin.gradle.plugin.KotlinBasePluginWrapper
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import java.util.*

class AndroidPlugin33 : AndroidPluginCompat() {

    override fun registerTransform(project: Project, debug: Property<Boolean>, hasKotlinPlugin: Boolean) {
        // For regular build and instrumentation (on mobile device) tests,
        // uses the Transform API for Android Plugin 7.1 and older.
        val androidExtension = project.extensions.findByType(BaseExtension::class.java)
            ?: error("The Android Gradle plugin BaseExtension was not found.")
        androidExtension.registerTransform(ObjectBoxAndroidTransform(debug))

        // For local (on dev machine) unit tests.
        // A transform registered like above does only run when dexing is required
        // (!= for local unit tests) so inject a custom transform task before local unit tests are compiled.
        // Note: could use findByType(TestedExtension::class.java), however, then do not
        // have access to compiled sources to add to transform task classpath.
        // Note: see ProjectEnv.androidPluginIds which plugins are supported.
        when (androidExtension) {
            is AppExtension -> androidExtension.applicationVariants.all {
                injectTransformTask(project, debug, hasKotlinPlugin, it, it.unitTestVariant)
            }
            // Used for Android Instant App base and feature modules, but deprecated as of
            // Android Plugin 3.4.0 (April 2019). Behaves similar to the library plugin.
            // https://developer.android.com/topic/google-play-instant/feature-module-migration
            is FeatureExtension -> androidExtension.featureVariants.all {
                injectTransformTask(project, debug, hasKotlinPlugin, it, it.unitTestVariant)
            }
            is LibraryExtension -> androidExtension.libraryVariants.all {
                injectTransformTask(project, debug, hasKotlinPlugin, it, it.unitTestVariant)
            }
            // Note: TestExtension is only used to create a separate instrumentation test module,
            // it can not run local unit tests.
            // https://developer.android.com/studio/test/advanced-test-setup#use-separate-test-modules-for-instrumented-tests
            // is TestExtension ->
        }
    }

    /**
     * Creates a task that transforms the variants JavaCompile and if available KotlinCompile output before the unit
     * test task for that variant runs. Transformed classes are stored in a dedicated build directory which is added
     * to the classpath of the test task.
     *
     * This mimics what registering a [Transform] does which only runs for
     * instrumented (on-device) tests and assembling an app/library
     * (bug report to support unit tests at https://issuetracker.google.com/issues/37076369).
     */
    private fun injectTransformTask(
        project: Project, debug: Property<Boolean>, hasKotlinPlugin: Boolean,
        baseVariant: BaseVariant, unitTestVariant: UnitTestVariant
    ) {
        // Add compiled Java project sources, makes Java compile task a dependency.
        // Note: javaCompileProvider requires at least Android Gradle Plugin 3.3.0
        val inputClasspath = project.files(baseVariant.javaCompileProvider.map { it.destinationDirectory })

        // Add compiled Java test sources, makes Java test compile task a dependency.
        inputClasspath.from(unitTestVariant.javaCompileProvider.map { it.destinationDirectory })

        // Same for Kotlin.
        // Applying Kotlin plugin is optional for Android projects, so check before accessing Kotlin plugin classes.
        if (hasKotlinPlugin) {
            project.plugins.withType(KotlinBasePluginWrapper::class.java) {
                addDestinationDirOfKotlinCompile(inputClasspath, project, baseVariant)
                addDestinationDirOfKotlinCompile(inputClasspath, project, unitTestVariant)
            }
        }

        val unitTestVariantNameCapitalized = unitTestVariant.name
            .replaceFirstChar { it.titlecase(Locale.getDefault()) }
        val transformTaskName = "objectboxTransform$unitTestVariantNameCapitalized"
        val outputDir = project.buildDir.resolve("intermediates/objectbox/${unitTestVariant.dirName}")
        // Use register to defer creation until use.
        val transformTask = project.tasks.register(
            transformTaskName,
            ObjectBoxTestClassesTransformTask::class.java,
            ObjectBoxTestClassesTransformTask.ConfigAction(debug, outputDir, inputClasspath)
        )

        // Configure the test classpath by appending the transform output file collection to the start of
        // the test classpath so the transformed files override the original ones. This also makes
        // the test task (the one that runs the tests) depend on the transform task.
        val outputFileCollection = project.files(transformTask.map { it.outputDir })
        val testTaskProvider = project.tasks.named("test$unitTestVariantNameCapitalized", Test::class.java)
        testTaskProvider.configure {
            it.classpath = outputFileCollection + it.classpath
        }
    }

    private fun addDestinationDirOfKotlinCompile(
        inputClasspath: ConfigurableFileCollection,
        project: Project,
        variant: BaseVariant
    ) {
        // Using naming scheme promised by https://kotlinlang.org/docs/reference/using-gradle.html#compiler-options
        val variantNameCapitalized = variant.name
            .replaceFirstChar { it.titlecase(Locale.getDefault()) }
        val kotlinTaskName = "compile${variantNameCapitalized}Kotlin"
        val kotlinCompileTaskProvider = project.tasks.named(kotlinTaskName, KotlinCompile::class.java)
        inputClasspath.from(kotlinCompileTaskProvider.map { it.destinationDirectory })
    }

}