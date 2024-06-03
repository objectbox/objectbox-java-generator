plugins {
    // https://github.com/ben-manes/gradle-versions-plugin/releases
    id("com.github.ben-manes.versions") version "0.46.0"
    // https://github.com/gradle-nexus/publish-plugin/releases
    id("io.github.gradle-nexus.publish-plugin") version "1.3.0"
    // https://github.com/gmazzo/gradle-buildconfig-plugin/releases
    id("com.github.gmazzo.buildconfig") version "4.0.3" apply false // code-modifier, gradle-plugin
}

buildscript {
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
    val kotlinVersion by extra("1.8.20") // Embedded by Gradle 8.2.1 used to compile this
    // To remain compatible with the lowest supported version of Gradle (see GradleCompat), set kotlinApiLevel to
    // the Kotlin language level supported by that version: https://docs.gradle.org/current/userguide/compatibility.html
    val kotlinApiLevel by extra("1.4") // Minimum supported Gradle 7.0 bundles Kotlin 1.4

    val essentialsVersion by extra("3.1.0")
    val javassistVersion by extra("3.29.2-GA")
    val junitVersion by extra("4.13.2") // https://junit.org/junit4/
    val truthVersion by extra("1.1.3") // https://github.com/google/truth/releases
    // mockito 5.0.0+ requires JDK 11
    val mockitoVersion by extra("4.11.0") // https://github.com/mockito/mockito/releases
    val moshiVersion by extra("1.15.0") // https://github.com/square/moshi/blob/master/CHANGELOG.md
    // okio 3.0.0+ requires Kotlin 1.5
    val okioVersion by extra("2.10.0") // https://github.com/square/okio/blob/master/CHANGELOG.md

    // Typically, only edit those two:
    val versionNumber = "4.0.1" // Without "-SNAPSHOT", e.g. "2.5.0" or "2.4.0-RC".
    val isRelease = true       // Set to true for releasing to ignore versionPostFix to avoid e.g. "-dev" versions.

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

    val objectboxPluginVersion by extra(versionNumber + versionPostFix) // Artifact versions of this project.
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
    this.repositories {
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
