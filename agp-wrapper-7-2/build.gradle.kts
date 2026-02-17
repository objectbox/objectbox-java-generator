import org.jetbrains.kotlin.gradle.dsl.KotlinVersion
import org.jetbrains.kotlin.gradle.tasks.KotlinJvmCompile

val kotlinApiLevel: String by rootProject.extra
val objectboxJavaVersion: String by rootProject.extra
val junitVersion: String by rootProject.extra
val truthVersion: String by rootProject.extra

plugins {
    kotlin("jvm")
    id("objectbox-publish")
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(8))
    }
    withJavadocJar()
    withSourcesJar()
}

tasks.withType<KotlinJvmCompile>().configureEach {
    compilerOptions {
        // Match Kotlin language level used by minimum supported Gradle version, see root build script for details.
        apiVersion.set(KotlinVersion.fromVersion(kotlinApiLevel))
    }
}

dependencies {
    api(project(":objectbox-code-modifier"))

    implementation(gradleApi())
    // Note: Kotlin plugin adds kotlin-stdlib-jdk8 dependency.

    implementation("io.objectbox:objectbox-java:$objectboxJavaVersion")
    val androidPluginVersion = "7.2.2"
    compileOnly("com.android.tools.build:gradle:$androidPluginVersion")
    compileOnly("com.android.tools.build:gradle-api:$androidPluginVersion")
    // https://asm.ow2.io/versions.html
    // See com.android.build.api.instrumentation.InstrumentationContext.getApiVersion
    // for ASM API versions that need to be supported.
    implementation("org.ow2.asm:asm-tree:9.3")

    testImplementation(testFixtures(project(":objectbox-code-modifier")))
    testImplementation("junit:junit:$junitVersion")
    testImplementation("com.google.truth:truth:$truthVersion")
    testImplementation("com.android.tools.build:gradle-api:$androidPluginVersion")
}

// Set project-specific properties
publishing {
    publications {
        getByName<MavenPublication>("mavenJava") {
            artifactId = "agp-wrapper-7-2"
            from(components["java"])
            pom {
                name.set("ObjectBox AGP Wrapper 7.2")
                description.set("Android Gradle Plugin Wrapper for ObjectBox")
            }
        }
    }
}
