plugins {
    // https://github.com/ben-manes/gradle-versions-plugin/releases
    id("com.github.ben-manes.versions") version "0.42.0"
    // https://github.com/gradle-nexus/publish-plugin/releases
    id("io.github.gradle-nexus.publish-plugin") version "1.1.0"
}

buildscript {
    // Kotlin version must match the one shipped with Gradle (see output of `gradlew --version`)
    // to avoid conflicts when compiling our Gradle plugin. https://youtrack.jetbrains.com/issue/KT-38010
    val kotlin_version by extra("1.4.20") // Gradle 6.8.3

    val android_version by extra("3.6.3") // See README for minimal supported version. http://google.github.io/android-gradle-dsl/javadoc/
    val essentials_version by extra("3.1.0")
    val junit_version by extra("4.13.2") // https://junit.org/junit4/
    // Note: truth 1.1.2 breaks Android plugin apply test.
    val truth_version by extra("1.0") // https://github.com/google/truth/releases
    val mockito_version by extra("3.8.0") // https://github.com/mockito/mockito/releases
    // moshi 1.13.0+ requires Kotlin 1.6.0
    val moshi_version by extra("1.12.0") // https://github.com/square/moshi/blob/master/CHANGELOG.md
    // okio 3.1.0+ requires Kotlin 1.6.20
    val okioVersion by extra("3.0.0") // https://github.com/square/okio/blob/master/CHANGELOG.md

    // Typically, only edit those two:
    val versionNumber = "3.2.1" // Without "-SNAPSHOT", e.g. "2.5.0" or "2.4.0-RC".
    val isRelease = false       // Set to true for releasing to ignore versionPostFix to avoid e.g. "-dev" versions.

    val libsRelease = isRelease  // e.g. diverge if plugin is still SNAPSHOT, but libs are already final
    val libsVersion = versionNumber + (if (libsRelease) "" else "-dev-SNAPSHOT")
    val libsSyncVersion = versionNumber + (if (libsRelease) "" else "-sync-SNAPSHOT")

    // Calculate version codes.
    val versionPostFix = if (isRelease) {
        ""
    } else if (project.hasProperty("versionPostFix")) {
        "-${project.property("versionPostFix")}-SNAPSHOT"
    } else {
        "-dev-SNAPSHOT"
    }

    val objectbox_plugin_version by extra(versionNumber + versionPostFix) // Artifact versions of this project.
    val objectbox_java_version by extra(libsVersion) // Java library used by sub-projects.
    val applies_ob_java_version by extra(libsVersion) // Java library added to projects applying the plugin.
    val applies_ob_native_version by extra(libsVersion) // Native library added to projects applying the ObjectBoxGradlePlugin.
    val applies_ob_native_sync_version by extra(libsSyncVersion) // Native library added to projects applying the ObjectBoxSyncGradlePlugin.

    println("version=$objectbox_plugin_version")
    println("objectbox_java_version=$objectbox_java_version")
    println("applies_ob_java_version=$applies_ob_java_version")
    println("ObjectBoxGradlePlugin:")
    println("  applies_ob_native_version=$applies_ob_native_version")
    println("ObjectBoxSyncGradlePlugin:")
    println("  applies_ob_native_sync_version=$applies_ob_native_sync_version\n")

    // Internal Maven repo: used in all projects, printing info/warning only once here.
    val hasInternalObjectBoxRepo by extra(project.hasProperty("gitlabUrl"))
    if (hasInternalObjectBoxRepo) {
        val gitlabUrl = project.property("gitlabUrl")
        println("gitlabUrl=$gitlabUrl added to repositories.")
    } else {
        println("WARNING: gitlabUrl missing from gradle.properties.")
    }

    repositories {
        mavenCentral()
        google()
        maven { url = uri("https://plugins.gradle.org/m2/") }
    }

    dependencies {
        classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:$kotlin_version")
        classpath("gradle.plugin.de.fuerstenau:BuildConfigPlugin:1.1.8") // for code-modifier, gradle-plugin
    }
}

fun isNonStable(version: String): Boolean {
    val stableKeyword = listOf("RELEASE", "FINAL", "GA").any { version.toUpperCase().contains(it) }
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
    val objectbox_plugin_version: String by rootProject.extra
    version = objectbox_plugin_version

    // Note: also update IncrementalCompilationTest.projectSetup as needed.
    repositories {
        mavenCentral()
        google()
        val hasInternalObjectBoxRepo: Boolean by rootProject.extra
        if (hasInternalObjectBoxRepo) {
            maven {
                val gitlabUrl = project.property("gitlabUrl")
                url = uri("$gitlabUrl/api/v4/groups/objectbox/-/packages/maven")
                name = "GitLab"
                credentials(HttpHeaderCredentials::class) {
                    name = project.findProperty("gitlabTokenName")?.toString() ?: "Private-Token"
                    value = project.findProperty("gitlabToken")?.toString()
                        ?: project.property("gitlabPrivateToken").toString()
                }
                authentication {
                    create<HttpHeaderAuthentication>("header")
                }
            }
        }
        mavenLocal()
    }

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

// Plugin to publish to Central https://github.com/gradle-nexus/publish-plugin/
// This plugin ensures a separate, named staging repo is created for each build when publishing.
nexusPublishing {
    repositories {
        sonatype {
            // Staging profile ID for io.objectbox is 1c4c69cbbab380
            // Get via https://oss.sonatype.org/service/local/staging/profiles
            // or with Nexus Staging Plugin getStagingProfile task.
            stagingProfileId.set("1c4c69cbbab380")
            if (project.hasProperty("sonatypeUsername") && project.hasProperty("sonatypePassword")) {
                println("nexusPublishing credentials supplied.")
                username.set(project.property("sonatypeUsername").toString())
                password.set(project.property("sonatypePassword").toString())
            } else {
                println("nexusPublishing credentials NOT supplied.")
            }
        }
    }
}
