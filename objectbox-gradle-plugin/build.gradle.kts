import org.gradle.kotlin.dsl.support.serviceOf

// https://docs.gradle.org/current/userguide/custom_plugins.html

val kotlinVersion: String by rootProject.extra
val kotlinApiLevel: String by rootProject.extra
val javassistVersion: String by rootProject.extra
val objectboxJavaVersion: String by rootProject.extra
val essentialsVersion: String by rootProject.extra
val junitVersion: String by rootProject.extra
val mockitoVersion: String by rootProject.extra
val truthVersion: String by rootProject.extra
val moshiVersion: String by rootProject.extra
val okioVersion: String by rootProject.extra

plugins {
    kotlin("jvm")
    id("com.github.gmazzo.buildconfig")
    id("objectbox-publish")
}

java {
    sourceCompatibility = JavaVersion.VERSION_1_8
    targetCompatibility = JavaVersion.VERSION_1_8
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>().configureEach {
    kotlinOptions {
        // Match Kotlin language level used by minimum supported Gradle version, see root build script for details.
        apiVersion = kotlinApiLevel
    }
}

/**
 * Create a new source set for testing, configures the implementation and runtimeOnly configuration to inherit all
 * dependencies from test, creates a test task.
 */
fun createTestKitSourceSet(name: String, testTaskDescription: String): TestKitSourceSetConfiguration {
    // https://docs.gradle.org/current/userguide/java_testing.html#sec:configuring_java_integration_tests
    val sourceSet = sourceSets.create("${name}Test") {
        // Add all main classes to the compile and runtime classpaths.
        compileClasspath += sourceSets.main.get().output
        runtimeClasspath += sourceSets.main.get().output
        // Add all test classes to the compile and runtime classpaths.
        compileClasspath += sourceSets.test.get().output
        runtimeClasspath += sourceSets.test.get().output
    }
    // Make implementation and runtimeOnly configuration inherit all dependencies from test.
    val testImplementation = configurations["${name}TestImplementation"]
    testImplementation.extendsFrom(configurations.testImplementation.get())
    val testRuntimeOnly = configurations["${name}TestRuntimeOnly"]
    testRuntimeOnly.extendsFrom(configurations.testRuntimeOnly.get())

    // Create a test task
    createTestKitTestTask("${name}Test", testTaskDescription, sourceSet)

    return TestKitSourceSetConfiguration(testImplementation, testRuntimeOnly)
}

data class TestKitSourceSetConfiguration(
    val testImplementation: Configuration,
    val testRuntimeOnly: Configuration,
)

fun createTestKitTestTask(name: String, description: String, sourceSet: SourceSet) {
    val testTask = tasks.register<Test>(name) {
        this.description = description
        group = "verification"

        testClassesDirs = sourceSet.output.classesDirs
        classpath = sourceSet.runtimeClasspath
    }
    configureTestTaskForTestKit(testTask)
    // Run test task as part of the check task.
    tasks.check { dependsOn(testTask) }
}

/**
 * Creates a task that creates a plugin classpath manifest named "plugin-classpath.txt" to inject the plugin classpath
 * into the TestKit GradleRunner. Also creates a configuration to support adding additional dependencies to
 * the classpath.
 */
// https://docs.gradle.org/6.0/userguide/test_kit.html#sub:test-kit-classpath-injection
fun createPluginClasspathFile(suffix: String = ""): PluginClassPathFile {
    val configuration = configurations.create("testPluginClasspath${suffix.capitalize()}")
    val createPluginClasspathFileTask = tasks.register("testPluginClasspath${suffix.capitalize()}File") {
        description = "Creates classpath manifest for the plugin."
        group = "verification"

        val outputDir = file("$buildDir/${this.name}")

        // Add main source set runtime classpath as task input.
        inputs.files(sourceSets.main.get().runtimeClasspath)
            .withPropertyName("runtimeClasspath")
            .withNormalizer(ClasspathNormalizer::class)
        // Register output directory as task output.
        outputs.dir(outputDir)
            .withPropertyName("outputDir")

        doLast {
            outputDir.mkdirs()
            // Adapted from PluginUnderTestMetadata task, make sure to prevent duplicates.
            // Get paths to JAR files from main classpath and from dependencies added to the above custom configuration.
            val pluginClasspath = sourceSets.main.get().runtimeClasspath.map { it.toString() }.toMutableSet()
            pluginClasspath.addAll(project.files(configuration).map { it.absolutePath })
            file("$outputDir/plugin-classpath.txt").writeText(
                pluginClasspath.joinToString("\n")
            )
        }
    }
    return PluginClassPathFile(configuration, createPluginClasspathFileTask)
}

data class PluginClassPathFile(
    val configuration: Configuration,
    val task: TaskProvider<Task>
)

// Configure default test task for TestKit (IncrementalCompilationTest).
configureTestTaskForTestKit(tasks.test)
// Test Android Plugin with
// - the lowest supported version and
// - with the latest API implemented (in the future might add tests for all API levels supported).
val (agp34TestImplementation, agp34TestRuntimeOnly) =
    createTestKitSourceSet("agp34", "Runs Android Plugin 3.4 integration tests.")
val (agp72TestImplementation, agp72TestRuntimeOnly) =
    createTestKitSourceSet("agp72", "Runs Android Plugin 7.2 integration tests.")

val (testPluginClasspath, testPluginClasspathFile) = createPluginClasspathFile()
val (testPluginClasspathAgp34, testPluginClasspathAgp34File) = createPluginClasspathFile("agp34")
val (testPluginClasspathAgp72, testPluginClasspathAgp72File) = createPluginClasspathFile("agp72")

dependencies {
    implementation(project(":objectbox-code-modifier"))
    implementation(project(":agp-wrapper-3-4"))
    implementation(project(":agp-wrapper-7-2"))

    implementation(gradleApi())
    // Note: Kotlin plugin adds kotlin-stdlib-jdk8 dependency.

    compileOnly("com.android.tools.build:gradle-api:7.2.0")
    compileOnly("org.jetbrains.kotlin:kotlin-gradle-plugin:$kotlinVersion")

    testImplementation(gradleTestKit())
    // For new Gradle TestKit tests (see GradleTestRunner).
    testRuntimeOnly(files(testPluginClasspathFile))
    val agp34Version = "3.4.3"
    testPluginClasspathAgp34("com.android.tools.build:gradle:$agp34Version")
    agp34TestRuntimeOnly(files(testPluginClasspathAgp34File))
    val agp72Version = "7.2.0"
    testPluginClasspathAgp72("com.android.tools.build:gradle:$agp72Version")
    agp72TestRuntimeOnly(files(testPluginClasspathAgp72File))
    
    // For plugin apply tests and outdated TestKit tests (dir "test-gradle-projects").
    testImplementation("org.jetbrains.kotlin:kotlin-gradle-plugin:$kotlinVersion")
    agp34TestRuntimeOnly("com.android.tools.build:gradle:$agp34Version")
    agp72TestRuntimeOnly("com.android.tools.build:gradle:$agp72Version")
    // Android Plugin 4.2.0 and higher require the BuildEventListenerFactory class,
    // which Gradle does not include by default, so manually add it.
    // https://github.com/gradle/gradle/issues/16774#issuecomment-853407822
    // https://issuetracker.google.com/issues/193859160
    agp72TestRuntimeOnly(
        files(
            serviceOf<org.gradle.api.internal.classpath.ModuleRegistry>().getModule("gradle-tooling-api-builders")
                .classpath.asFiles.first()
        )
    )

    testImplementation("io.objectbox:objectbox-java:$objectboxJavaVersion")
    testImplementation("org.greenrobot:essentials:$essentialsVersion")
    testImplementation("junit:junit:$junitVersion")
    testImplementation("org.mockito:mockito-core:$mockitoVersion")
    testImplementation("com.google.truth:truth:$truthVersion")
    testImplementation("com.squareup.moshi:moshi:$moshiVersion")
    testImplementation("com.squareup.okio:okio:$okioVersion")
    testImplementation("org.javassist:javassist:$javassistVersion")
}

val appliesObxJavaVersion: String by rootProject.extra
val appliesObxJniLibVersion: String by rootProject.extra
val appliesObxSyncJniLibVersion: String by rootProject.extra

buildConfig {
    // rename to avoid conflict with other build config files (modules use same root package)
    className("GradlePluginBuildConfig")
    packageName("io.objectbox")

    buildConfigField("String", "VERSION", provider { "\"${project.version}\"" })
    // Versions of libraries to add to projects applying the plugin.
    buildConfigField("String", "APPLIES_JAVA_VERSION", provider { "\"$appliesObxJavaVersion\"" })
    buildConfigField("String", "APPLIES_NATIVE_VERSION", provider { "\"$appliesObxJniLibVersion\"" })
    buildConfigField("String", "APPLIES_NATIVE_SYNC_VERSION", provider { "\"$appliesObxSyncJniLibVersion\"" })
}

val javadocJar by tasks.registering(Jar::class) {
    archiveClassifier.set("javadoc")
    from("README")
}

val sourcesJar by tasks.registering(Jar::class) {
    archiveClassifier.set("sources")
    from("README")
}

// Need to evaluate other modules before a publication for them can be created below.
evaluationDependsOn(":objectbox-code-modifier")
evaluationDependsOn(":objectbox-generator")
evaluationDependsOn(":objectbox-processor")

publishing {
    publications {
        // A test repository used for integration tests of this module.
        repositories {
            maven {
                url = uri("$buildDir/repository")
                name = "test"
            }
        }
        // Publications for the modules required by integration tests, depends on their projects being evaluated before.
        publications {
            create<MavenPublication>("modifier") {
                val fromProject = project(":objectbox-code-modifier")
                from(fromProject.components["java"])
                groupId = fromProject.group.toString()
                artifactId = fromProject.name
                version = fromProject.version.toString()
            }
            create<MavenPublication>("generator") {
                val fromProject = project(":objectbox-generator")
                from(fromProject.components["java"])
                groupId = fromProject.group.toString()
                artifactId = fromProject.name
                version = fromProject.version.toString()
            }
            create<MavenPublication>("processor") {
                val fromProject = project(":objectbox-processor")
                from(fromProject.components["java"])
                groupId = fromProject.group.toString()
                artifactId = fromProject.name
                version = fromProject.version.toString()
            }
        }

        // Set project-specific properties for the publication of this module.
        getByName<MavenPublication>("mavenJava") {
            from(components["java"])
            artifact(sourcesJar)
            artifact(javadocJar)
            pom {
                name.set("ObjectBox Gradle Plugin")
                description.set("Gradle Plugin for ObjectBox (NoSQL for Objects)")
            }
        }
    }
}

/**
 * Configures the given test task to depend on publishing of other modules to the test repository
 * and to forward some project properties to access the internal GitLab repository.
 */
fun configureTestTaskForTestKit(testTaskProvider: TaskProvider<Test>) {
    testTaskProvider.configure {
        // Register the jar task output of the other modules as task inputs (to detect changes).
        inputs.files(
            project(":objectbox-code-modifier").tasks.named("jar"),
            project(":objectbox-generator").tasks.named("jar"),
            project(":objectbox-processor").tasks.named("jar")
        )
        // Publish the other modules to the test repository before running this test task.
        dependsOn(
            "publishModifierPublicationToTestRepository",
            "publishGeneratorPublicationToTestRepository",
            "publishProcessorPublicationToTestRepository"
        )
        // Forward project properties required for TestKit tests to access the internal GitLab repository.
        systemProperty("gitlabUrl", project.findProperty("gitlabUrl") ?: "")
        systemProperty("gitlabTokenName", project.findProperty("gitlabTokenName") ?: "Private-Token")
        systemProperty(
            "gitlabToken",
            project.findProperty("gitlabToken") ?: project.findProperty("gitlabPrivateToken") ?: ""
        )
    }
}
