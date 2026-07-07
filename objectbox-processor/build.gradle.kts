import org.jetbrains.kotlin.gradle.dsl.JvmTarget

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
    // Note: the javadoc JAR is created using a custom task defined in the objectbox-publish plugin
    withSourcesJar()
}

tasks.withType<JavaCompile>().configureEach {
    options.release.set(8)
}

kotlin {
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

    // For the KSP processor: compile against an API version built with a Kotlin version this build
    // can read metadata of. At runtime the consumer project supplies its (newer) KSP version, KSP
    // keeps the processor API compatible (verified by TestKit tests running the supported KSP version).
    compileOnly("com.google.devtools.ksp:symbol-processing-api:2.0.21-1.0.28")

    // manually add tools.jar required by compile-testing
    val toolsJar = org.gradle.internal.jvm.Jvm.current().toolsJar
    if (toolsJar != null) {
        testCompileOnly(files(toolsJar))
    }
    // To run the KSP processor in tests, without Gradle: the standalone KSP2 implementation
    // (bundles the Kotlin Analysis API). Like the compileOnly API dependency of the processor,
    // the version must match the Kotlin version of this build.
    testImplementation("com.google.devtools.ksp:symbol-processing-api:2.0.21-1.0.28")
    testImplementation("com.google.devtools.ksp:symbol-processing-common-deps:2.0.21-1.0.28")
    testImplementation("com.google.devtools.ksp:symbol-processing-aa:2.0.21-1.0.28")
    testImplementation("junit:junit:$junitVersion")
    testImplementation("com.google.truth:truth:$truthVersion")
    // https://github.com/google/compile-testing/releases
    // compile-testing 0.20.0+ requires auto-value 1.10 which requires Kotlin 1.7 binary code.
    testImplementation("com.google.testing.compile:compile-testing:0.19")
    // generated files during test need objectbox dependencies to compile
    testImplementation("io.objectbox:objectbox-java:$objectboxJavaVersion")
}

// Set project-specific properties
publishing {
    publications {
        create<MavenPublication>("obxProcessor") {
            artifactId = "objectbox-processor"
            from(components["java"])
            artifact(tasks.named("javadocReadmeJar")) // task registered by objectbox-publish plugin
            pom {
                // Note: common configuration is set by objectbox-publish plugin
                packaging = "jar"
                name.set("ObjectBox Processor")
                description.set("Annotation processor for ObjectBox (NoSQL for Objects)")
            }
        }
    }
}
