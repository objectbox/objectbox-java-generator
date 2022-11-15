import org.gradle.api.internal.classpath.ModuleRegistry
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

val testPluginClasspath: Configuration by configurations.creating

// For integration tests (TestKit): Write the plugin's classpath to a file to share with the tests.
// https://docs.gradle.org/6.0/userguide/test_kit.html#sub:test-kit-classpath-injection
val createClasspathManifest by tasks.registering {
    val outputDir = file("$buildDir/$name")

    inputs.files(sourceSets.main.get().runtimeClasspath)
        .withPropertyName("runtimeClasspath")
        .withNormalizer(ClasspathNormalizer::class)
    outputs.dir(outputDir)
        .withPropertyName("outputDir")

    doLast {
        outputDir.mkdirs()
        // Adapted from PluginUnderTestMetadata task, make sure to prevent duplicates.
        val pluginClasspath = sourceSets.main.get().runtimeClasspath.map { it.toString() }.toMutableSet()
        pluginClasspath.addAll(project.files(testPluginClasspath).map { it.absolutePath })
        file("$outputDir/plugin-classpath.txt").writeText(
            pluginClasspath.joinToString("\n")
        )
    }
}

dependencies {
    implementation(project(":objectbox-code-modifier"))
    implementation(project(":agp-wrapper-3-3"))
    implementation(project(":agp-wrapper-7-2"))

    implementation(gradleApi())
    // Note: Kotlin plugin adds kotlin-stdlib-jdk8 dependency.

    compileOnly("com.android.tools.build:gradle-api:7.2.0")
    compileOnly("org.jetbrains.kotlin:kotlin-gradle-plugin:$kotlinVersion")

    testImplementation(gradleTestKit())
    // For new Gradle TestKit tests (see GradleTestRunner).
    testRuntimeOnly(files(createClasspathManifest))
    val testedAndroidPluginVersion = "7.2.0"
    testPluginClasspath("com.android.tools.build:gradle:$testedAndroidPluginVersion")
    // For plugin apply tests and outdated TestKit tests (dir "test-gradle-projects").
    testImplementation("org.jetbrains.kotlin:kotlin-gradle-plugin:$kotlinVersion")
    testImplementation("com.android.tools.build:gradle:$testedAndroidPluginVersion")
    // Android Plugin 4.2.0 and higher require the BuildEventListenerFactory class,
    // which Gradle does not include by default, so manually add it.
    // https://github.com/gradle/gradle/issues/16774#issuecomment-853407822
    // https://issuetracker.google.com/issues/193859160
    testRuntimeOnly(
        files(
            serviceOf<ModuleRegistry>().getModule("gradle-tooling-api-builders")
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

// For integration tests (TestKit): publish other modules to repository in build folder.
evaluationDependsOn(":objectbox-code-modifier")
evaluationDependsOn(":objectbox-generator")
evaluationDependsOn(":objectbox-processor")
publishing {
    repositories {
        maven {
            url = uri("$buildDir/repository")
            name = "test"
        }
    }
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
}
tasks {
    test {
        inputs.files(
            project(":objectbox-code-modifier").tasks.named("jar"),
            project(":objectbox-generator").tasks.named("jar"),
            project(":objectbox-processor").tasks.named("jar")
        )
        dependsOn(
            "publishModifierPublicationToTestRepository",
            "publishGeneratorPublicationToTestRepository",
            "publishProcessorPublicationToTestRepository"
        )

        // For integration tests (TestKit): steal project properties required for TestKit projects.
        systemProperty("gitlabUrl", project.findProperty("gitlabUrl") ?: "")
        systemProperty("gitlabTokenName", project.findProperty("gitlabTokenName") ?: "Private-Token")
        systemProperty(
            "gitlabToken",
            project.findProperty("gitlabToken") ?: project.findProperty("gitlabPrivateToken") ?: ""
        )
    }
}

val javadocJar by tasks.registering(Jar::class) {
    archiveClassifier.set("javadoc")
    from("README")
}

val sourcesJar by tasks.registering(Jar::class) {
    archiveClassifier.set("sources")
    from("README")
}

// Set project-specific properties
publishing {
    publications {
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
