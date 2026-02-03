
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinJvmCompile

plugins {
    id("java")
    kotlin("jvm")
    id("objectbox-publish")
    id("objectbox-disable-analytics")
}

// Tests require JDK 11, so set toolchain to 11 but still only allow and compile Java 8 code.
// https://docs.gradle.org/current/userguide/building_java_projects.html#sec:java_cross_compilation
java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(11))
    }
}

tasks.withType<JavaCompile>().configureEach {
    options.release.set(8)
}

tasks.withType<KotlinJvmCompile>().configureEach {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_1_8)
    }
}

val objectboxJavaVersion: String by rootProject.extra
val junitVersion: String by rootProject.extra
val truthVersion: String by rootProject.extra

dependencies {
    implementation(project(":objectbox-code-modifier"))
    implementation(project(":objectbox-generator"))

    // Note: Kotlin plugin adds kotlin-stdlib-jdk8 dependency.

    implementation("io.objectbox:objectbox-java-api:$objectboxJavaVersion")
    implementation("io.objectbox:objectbox-java:$objectboxJavaVersion")

    // manually add tools.jar required by compile-testing
    val toolsJar = org.gradle.internal.jvm.Jvm.current().toolsJar
    if (toolsJar != null) {
        testCompileOnly(files(toolsJar))
    }
    testImplementation("junit:junit:$junitVersion")
    testImplementation("com.google.truth:truth:$truthVersion")
    // https://github.com/google/compile-testing/releases
    // compile-testing 0.20.0+ requires auto-value 1.10 which requires Kotlin 1.7 binary code.
    testImplementation("com.google.testing.compile:compile-testing:0.19")
    // generated files during test need objectbox dependencies to compile
    testImplementation("io.objectbox:objectbox-java:$objectboxJavaVersion")
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
            artifactId = "objectbox-processor"
            from(components["java"])
            artifact(sourcesJar)
            artifact(javadocJar)
            pom {
                name.set("ObjectBox Processor")
                description.set("Annotation processor for ObjectBox (NoSQL for Objects)")
            }
        }
    }
}
