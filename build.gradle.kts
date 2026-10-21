/*
 * This script supports some Gradle project properties:
 *
 * - versionSuffix: appended to snapshot version number, e.g. "1.2.3-<versionSuffix>-SNAPSHOT".
 *   Use to create different versions based on branch/tag.
 * - sonatypeUsername: Maven Central credential used by Nexus publishing.
 * - sonatypePassword: Maven Central credential used by Nexus publishing.
 *
 * This script supports the following environment variables:
 *
 * - OBX_RELEASE: If set to "true" builds and depends on release versions, without branch name and snapshot suffix.
 */

// Gradle properties (more defined in buildscript block below)
val propertySonatypeUsername = providers.gradleProperty("sonatypeUsername")
val propertySonatypePassword = providers.gradleProperty("sonatypePassword")

plugins {
    // https://github.com/ben-manes/gradle-versions-plugin/releases
    id("com.github.ben-manes.versions") version "0.53.0"
    // https://github.com/gradle-nexus/publish-plugin/releases
    id("io.github.gradle-nexus.publish-plugin") version "2.0.0"
    // https://github.com/gmazzo/gradle-buildconfig-plugin/releases
    id("com.github.gmazzo.buildconfig") version "6.0.7" apply false // code-modifier, gradle-plugin
}

buildscript {
    // Environment variables (see notes at the top of this file)
    // https://docs.gitlab.com/ci/variables/predefined_variables/
    val envIsCI: Boolean = System.getenv("CI") == "true"
    val envRelease = System.getenv("OBX_RELEASE")
    // Gradle properties (see notes at the top of this file)
    val propertyVersionSuffixName = "versionSuffix"
    val propertyVersionSuffix = providers.gradleProperty(propertyVersionSuffixName)

    // Version of Maven artifacts
    // Should only be changed as part of the release process, see the release checklist in the objectbox repo
    val versionNumber = "6.0.0"

    // If OBX_RELEASE is set, build and depend on release versions. Doesn't publish a release.
    // See the release checklist in the objectbox repo on how to publish a release.
    // If true, Maven artifacts use a release version, so without branch name and snapshot suffix
    // (such as "-dev-SNAPSHOT"), including for dependencies (such as objectbox-java).
    val isRelease = envRelease == "true"

    val libsRelease = isRelease // e.g. diverge if plugin is still SNAPSHOT, but libs are already final
    val libsVersion = versionNumber + (if (libsRelease) "" else "-dev-SNAPSHOT")
    val libsSyncVersion = versionNumber + (if (libsRelease) "" else "-sync-SNAPSHOT")

    // If not releasing, produce snapshot artifacts and add the branch name to the version string
    // (passed in by CI through the versionSuffix property).
    val versionSuffix = if (isRelease) {
        ""
    } else if (propertyVersionSuffix.isPresent) {
        "-${propertyVersionSuffix.get()}-SNAPSHOT"
    } else {
        if (envIsCI) {
            throw GradleException("Publishing: property $propertyVersionSuffixName must be set in CI to calculate version suffix.")
        }
        println("WARNING: Publishing: property $propertyVersionSuffixName not set, using '-dev-SNAPSHOT' version suffix.")
        "-dev-SNAPSHOT"
    }

    val objectboxPluginVersion by extra(versionNumber + versionSuffix) // Artifact versions of this project.
    val objectboxJavaVersion by extra(libsVersion) // Java library used by sub-projects.
    val appliesObxJavaVersion by extra(libsVersion) // Java library added to projects applying the plugin.
    val appliesObxJniLibVersion by extra(libsVersion) // Native library added to projects applying the ObjectBoxGradlePlugin.
    val appliesObxSyncJniLibVersion by extra(libsSyncVersion) // Native library added to projects applying the ObjectBoxSyncGradlePlugin.

    println("version=$objectboxPluginVersion")
    println("objectboxJavaVersion=$objectboxJavaVersion")
    println("appliesObxJavaVersion=$appliesObxJavaVersion")
    println("ObjectBoxGradlePlugin:")
    println("  appliesObxJniLibVersion=$appliesObxJniLibVersion")
    println("ObjectBoxSyncGradlePlugin:")
    println("  appliesObxSyncJniLibVersion=$appliesObxSyncJniLibVersion\n")

    // Kotlin compiler and API level
    //
    // Gradle runs (pre-compiled) binary plugins, like the one in this project, using its embedded
    // version of the Kotlin standard library. Regardless of what version a plugin was compiled for
    // and depends on. (Script plugins are compiled at build time using an even older language level.)
    // https://blog.gradle.org/three-kotlin-versions-in-a-gradle-project
    //
    // So to be compatible with Gradle, all subprojects that are run as part of the plugin (so not
    // the annotation processor and generator) must set the Kotlin API version compiler option to
    // prevent using APIs not supported by the Kotlin version Gradle embeds. Note that dependencies
    // also must not use newer Kotlin APIs!
    //
    // To also remain compatible with the latest Gradle versions, this project should compile with
    // the latest minor version of Kotlin embedded by Gradle. This prevents Kotlin API
    // incompatibilities, such as the removal of deprecated functions.
    //
    // So for simplicity and to avoid other compiler issues in this project, the Kotlin version
    // embedded by the Gradle version that compiles this project is used.

    // Matches the Kotlin version embedded by the Gradle version that compiles this project.
    // Details above.
    // Look it up at https://docs.gradle.org/current/userguide/compatibility.html#kotlin
    // or with `gradlew --version`.
    // Gradle 9.5.1 embeds Kotlin 2.3.20
    val kotlinVersion by extra("2.3.20")
    // At least the lowest supported by kotlinVersion (and project dependencies). Determines the
    // minimum supported Gradle version based on the Kotlin version it embeds. Details above.
    // Look up in org.jetbrains.kotlin.gradle.dsl.KotlinVersion (check deprecated annotations).
    // Kotlin 2.3.20 still supports 1.9, requires at least Gradle 8.3 which embeds Kotlin 1.9.0
    val kotlinApiVersion by extra("1.9")

    val essentialsVersion by extra("3.1.0")
    // 3.24.0-GA and newer support Java 11 byte code
    val javassistVersion by extra("3.30.2-GA") // https://github.com/jboss-javassist/javassist/releases
    val junitVersion by extra("4.13.2") // https://junit.org/junit4/
    val truthVersion by extra("1.4.5") // https://github.com/google/truth/releases
    // mockito 5.0.0+ requires JDK 11
    val mockitoVersion by extra("4.11.0") // https://github.com/mockito/mockito/releases
    val moshiVersion by extra("1.15.2") // https://github.com/square/moshi/blob/master/CHANGELOG.md
    // The KSP Gradle plugin version recommended by (and tested with) the ObjectBox plugin for Kotlin
    // projects. KSP versions independently of Kotlin since 2.3.0.
    // Also used to run the moshi-kotlin-codegen annotation processor (kapt does not support Kotlin 2.3 metadata).
    // Moshi doesn't support KSP 2 until v2, currently in preview, but out of luck it appears this
    // project isn't affected by the changed return values of KSP 2
    // (https://github.com/google/ksp/blob/main/docs/ksp2api.md) so use it regardless.
    val kspVersion by extra("2.3.9") // https://github.com/google/ksp/releases
    // okio 3.0.0+ requires Kotlin 1.5
    val okioVersion by extra("2.10.0") // https://github.com/square/okio/blob/master/CHANGELOG.md

    dependencies {
        classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:$kotlinVersion")
        classpath("com.google.devtools.ksp:symbol-processing-gradle-plugin:$kspVersion")
    }
}

fun isNonStable(version: String): Boolean {
    val stableKeyword = listOf("RELEASE", "FINAL", "GA").any { version.uppercase().contains(it) }
    val regex = "^[0-9,.v-]+(-r)?$".toRegex()
    val isStable = stableKeyword || regex.matches(version)
    return isStable.not()
}

tasks.withType<com.github.benmanes.gradle.versions.updates.DependencyUpdatesTask> {
    rejectVersionIf {
        isNonStable(candidate.version)
    }
}

allprojects {
    group = "io.objectbox"
    val objectboxPluginVersion: String by rootProject.extra
    version = objectboxPluginVersion

    configurations.all {
        // Projects are using snapshot dependencies that may update more often than 24 hours.
        resolutionStrategy {
            cacheChangingModulesFor(0, "seconds")
        }
    }
}

tasks.wrapper {
    distributionType = Wrapper.DistributionType.ALL
}

// Plugin to publish to Maven Central https://github.com/gradle-nexus/publish-plugin/
// This plugin ensures a separate, named staging repo is created for each build when publishing.
nexusPublishing {
    this.repositories {
        sonatype {
            // Use the Portal OSSRH Staging API as this plugin does not support the new Portal API
            // https://central.sonatype.org/publish/publish-portal-ossrh-staging-api/#configuring-your-plugin
            nexusUrl.set(uri("https://ossrh-staging-api.central.sonatype.com/service/local/"))
            snapshotRepositoryUrl.set(uri("https://central.sonatype.com/repository/maven-snapshots/"))

            if (propertySonatypeUsername.isPresent && propertySonatypePassword.isPresent) {
                println("Publishing: Maven Central credentials supplied")
                username.set(propertySonatypeUsername.get())
                password.set(propertySonatypePassword.get())
            } else {
                println("Publishing: Maven Central credentials NOT supplied, see root build script for required project properties")
            }
        }
    }
}
