val kotlinVersion: String by rootProject.extra
val kotlinApiLevel: String by rootProject.extra

plugins {
    kotlin("jvm")
    id("objectbox-publish")
}

java {
    sourceCompatibility = JavaVersion.VERSION_1_8
    targetCompatibility = JavaVersion.VERSION_1_8
    withJavadocJar()
    withSourcesJar()
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>().configureEach {
    kotlinOptions {
        // Match Kotlin language level used by minimum supported Gradle version, see root build script for details.
        apiVersion = kotlinApiLevel
    }
}

dependencies {
    api(project(":objectbox-code-modifier"))

    implementation(gradleApi())
    // Note: Kotlin plugin adds kotlin-stdlib-jdk8 dependency.

    compileOnly("com.android.tools.build:gradle:3.3.0") {
        // Exclude transient kotlin-reflect to avoid conflicts with this projects version.
        exclude(group = "org.jetbrains.kotlin", module = "kotlin-reflect")
        // Exclude builder dependency as it bundles the Kotlin library and this only needs the plugin APIs.
        exclude(group = "com.android.tools.build", module = "builder")
    }
    compileOnly("org.jetbrains.kotlin:kotlin-gradle-plugin:$kotlinVersion")
}

// Set project-specific properties
publishing {
    publications {
        getByName<MavenPublication>("mavenJava") {
            artifactId = "agp-wrapper-3-3"
            from(components["java"])
            pom {
                name.set("ObjectBox AGP Wrapper 3.3.0")
                description.set("Android Gradle Plugin Wrapper for ObjectBox")
            }
        }
    }
}
