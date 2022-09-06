val kotlinVersion: String by rootProject.extra

plugins {
    id("org.jetbrains.kotlin.jvm")
}

java {
    sourceCompatibility = JavaVersion.VERSION_1_8
    targetCompatibility = JavaVersion.VERSION_1_8
    withJavadocJar()
    withSourcesJar()
}

dependencies {
    implementation(gradleApi())
    api(project(":objectbox-code-modifier"))
    compileOnly("com.android.tools.build:gradle:3.3.0")
    // Note: override kotlin-reflect version from com.android.tools.build:gradle to avoid mismatch with stdlib above.
    implementation("org.jetbrains.kotlin:kotlin-reflect:$kotlinVersion")
    compileOnly("org.jetbrains.kotlin:kotlin-gradle-plugin:$kotlinVersion")
}

apply(from = rootProject.file("gradle/objectbox-publish.gradle"))
// Set project-specific properties
configure<PublishingExtension> {
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
