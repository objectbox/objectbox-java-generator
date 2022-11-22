plugins {
    id("java")
    kotlin("jvm")
    id("objectbox-publish")
}

java {
    sourceCompatibility = JavaVersion.VERSION_1_8
    targetCompatibility = JavaVersion.VERSION_1_8
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
    // auto-service generates service configuration for annotation processor.
    val autoService = "1.0.1"
    compileOnly("com.google.auto.service:auto-service-annotations:$autoService")
    annotationProcessor("com.google.auto.service:auto-service:$autoService")
    // incap generates the META-INF descriptor required to enable incremental annotation processing with Gradle.
    val incap = "0.3"
    implementation("net.ltgt.gradle.incap:incap:$incap")
    annotationProcessor("net.ltgt.gradle.incap:incap-processor:$incap")

    // manually add tools.jar required by compile-testing
    val toolsJar = org.gradle.internal.jvm.Jvm.current().toolsJar
    if (toolsJar != null) {
        testCompileOnly(files(toolsJar))
    }
    testImplementation("junit:junit:$junitVersion")
    testImplementation("com.google.truth:truth:$truthVersion")
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
