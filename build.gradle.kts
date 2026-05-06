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
    val envRelease = System.getenv("OBX_RELEASE")
    // Gradle properties (see notes at the top of this file)
    val propertyVersionSuffix = providers.gradleProperty("versionSuffix")

    // Version of Maven artifacts
    // Should only be changed as part of the release process, see the release checklist in the objectbox repo
    val versionNumber = "5.4.3"

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

    // Note: Gradle runs plugins at the Kotlin language level that Gradle version supports using the Kotlin library it
    // embeds, regardless of what Kotlin library the plugin depends on.
    // https://github.com/gradle/gradle/issues/16345#issuecomment-931437640
    // https://docs.gradle.org/current/userguide/compatibility.html
    // To ensure compatibility, all plugin projects (so excluding the annotation processor and generator) set a Kotlin
    // API level to avoid using Kotlin APIs not supported when run by Gradle. Note that dependencies also must not
    // use newer Kotlin APIs!

    // To remain compatible with new Gradle versions, this project should aim to compile with a Gradle version
    // using the highest supported Kotlin language level to detect Kotlin API incompatibilities (e.g. removal of
    // deprecated functions). So far only major Gradle releases have changed the Kotlin language level.
    // Set kotlinVersion to the Kotlin version embedded by the Gradle version used to compile this project (needs to
    // be the exact version to avoid conflicts):
    // https://docs.gradle.org/current/userguide/compatibility.html or see output of `gradlew --version`
    val kotlinVersion by extra("2.0.21") // Embedded by Gradle 8.14.4 used to compile this
    // To remain compatible with the lowest supported version of Gradle (see GradleCompat), set kotlinApiLevel to
    // the Kotlin language level supported by that version: https://docs.gradle.org/current/userguide/compatibility.html
    val kotlinApiLevel by extra("1.4") // Minimum supported Gradle 7.0 bundles Kotlin 1.4

    val essentialsVersion by extra("3.1.0")
    // 3.24.0-GA and newer support Java 11 byte code
    val javassistVersion by extra("3.30.2-GA") // https://github.com/jboss-javassist/javassist/releases
    val junitVersion by extra("4.13.2") // https://junit.org/junit4/
    val truthVersion by extra("1.4.5") // https://github.com/google/truth/releases
    // mockito 5.0.0+ requires JDK 11
    val mockitoVersion by extra("4.11.0") // https://github.com/mockito/mockito/releases
    val moshiVersion by extra("1.15.0") // https://github.com/square/moshi/blob/master/CHANGELOG.md
    // okio 3.0.0+ requires Kotlin 1.5
    val okioVersion by extra("2.10.0") // https://github.com/square/okio/blob/master/CHANGELOG.md

    dependencies {
        classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:$kotlinVersion")
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
