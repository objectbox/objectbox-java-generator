plugins {
    id("java")
    id("kotlin")
}

java {
    sourceCompatibility = JavaVersion.VERSION_1_8
    targetCompatibility = JavaVersion.VERSION_1_8
}

val kotlin_version: String by rootProject.extra
val objectbox_java_version: String by rootProject.extra
val junit_version: String by rootProject.extra
val truth_version: String by rootProject.extra

dependencies {
    implementation(project(":objectbox-code-modifier"))
    implementation(project(":objectbox-generator"))

    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8:$kotlin_version")
    implementation("org.jetbrains.kotlin:kotlin-reflect:$kotlin_version")
    implementation("io.objectbox:objectbox-java-api:$objectbox_java_version")
    implementation("io.objectbox:objectbox-java:$objectbox_java_version")
    // auto-service generates service configuration for annotation processor.
    val autoService = "1.0"
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
    testImplementation("junit:junit:$junit_version")
    testImplementation("com.google.truth:truth:$truth_version")
    testImplementation("com.google.testing.compile:compile-testing:0.19")
    // generated files during test need objectbox dependencies to compile
    testImplementation("io.objectbox:objectbox-java:$objectbox_java_version")
}

val javadocJar by tasks.registering(Jar::class) {
    archiveClassifier.set("javadoc")
    from("README")
}

val sourcesJar by tasks.registering(Jar::class) {
    archiveClassifier.set("sources")
    from("README")
}

apply(from = rootProject.file("gradle/objectbox-publish.gradle"))
// Set project-specific properties
configure<PublishingExtension> {
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
