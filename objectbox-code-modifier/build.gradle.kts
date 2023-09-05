import org.jetbrains.kotlin.gradle.dsl.KotlinVersion
import org.jetbrains.kotlin.gradle.tasks.KotlinJvmCompile

val kotlinVersion: String by rootProject.extra
val kotlinApiLevel: String by rootProject.extra
val essentialsVersion: String by rootProject.extra
val javassistVersion: String by rootProject.extra
val moshiVersion: String by rootProject.extra
val okioVersion: String by rootProject.extra
val objectboxJavaVersion: String by rootProject.extra
val junitVersion: String by rootProject.extra
val mockitoVersion: String by rootProject.extra
val truthVersion: String by rootProject.extra

plugins {
    id("java-library")
    id("java-test-fixtures")
    kotlin("jvm")
    kotlin("kapt")
    id("com.github.gmazzo.buildconfig")
    id("objectbox-publish")
    id("objectbox-disable-analytics")
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(8))
    }
}

tasks.withType<KotlinJvmCompile>().configureEach {
    compilerOptions {
        // Match Kotlin language level used by minimum supported Gradle version, see root build script for details.
        apiVersion.set(KotlinVersion.fromVersion(kotlinApiLevel))
    }
}

dependencies {
    implementation(gradleApi())
    // Note: Kotlin plugin adds kotlin-stdlib-jdk8 dependency.

    implementation("org.greenrobot:essentials:$essentialsVersion")
    implementation("org.javassist:javassist:$javassistVersion")
    implementation("com.squareup.moshi:moshi:$moshiVersion")
    kapt("com.squareup.moshi:moshi-kotlin-codegen:$moshiVersion")
    implementation("com.squareup.okio:okio:$okioVersion")

    implementation("io.objectbox:objectbox-java:$objectboxJavaVersion")

    testImplementation("junit:junit:$junitVersion")
    testImplementation("org.mockito:mockito-core:$mockitoVersion")
    testImplementation("com.google.truth:truth:$truthVersion")
    testImplementation("org.jetbrains.kotlin:kotlin-reflect:$kotlinVersion")

    testFixturesImplementation("io.objectbox:objectbox-java:$objectboxJavaVersion")
}

buildConfig {
    // rename to avoid conflict with other build config files (modules use same root package)
    className("CodeModifierBuildConfig")
    packageName("io.objectbox")

    buildConfigField("String", "VERSION", provider { "\"${project.version}\"" })
}

val javadocJar by tasks.registering(Jar::class) {
    archiveClassifier.set("javadoc")
    from("README")
}

val sourcesJar by tasks.registering(Jar::class) {
    archiveClassifier.set("sources")
    from("README")
}

// Do not publish test fixtures.
val javaComponent = components["java"] as AdhocComponentWithVariants
javaComponent.withVariantsFromConfiguration(configurations["testFixturesApiElements"]) { skip() }
javaComponent.withVariantsFromConfiguration(configurations["testFixturesRuntimeElements"]) { skip() }

// Set project-specific properties
publishing {
    publications {
        getByName<MavenPublication>("mavenJava") {
            artifactId = "objectbox-code-modifier"
            from(components["java"])
            artifact(sourcesJar)
            artifact(javadocJar)
            pom {
                name.set("ObjectBox Code Modifier")
                description.set("Code modifier for ObjectBox (NoSQL for Objects)")
            }
        }
    }
}
