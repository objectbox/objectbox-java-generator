val objectboxJavaVersion: String by rootProject.extra
val junitVersion: String by rootProject.extra
val truthVersion: String by rootProject.extra

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
    implementation("io.objectbox:objectbox-java:$objectboxJavaVersion")
    val androidPluginVersion = "7.2.0"
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
