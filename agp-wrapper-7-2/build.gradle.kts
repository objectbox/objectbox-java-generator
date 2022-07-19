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
    compileOnly("com.android.tools.build:gradle:7.2.0")
    // https://asm.ow2.io/versions.html
    implementation("org.ow2.asm:asm:9.3")
}

apply(from = rootProject.file("gradle/objectbox-publish.gradle"))
// Set project-specific properties
configure<PublishingExtension> {
    publications {
        getByName<MavenPublication>("mavenJava") {
            artifactId = "agp-wrapper-7-2"
            from(components["java"])
            pom {
                name.set("ObjectBox AGP Wrapper 7.2.0")
                description.set("Android Gradle Plugin Wrapper for ObjectBox")
            }
        }
    }
}
