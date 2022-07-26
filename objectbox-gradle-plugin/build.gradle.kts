// https://docs.gradle.org/current/userguide/custom_plugins.html

plugins {
    id("kotlin")
    id("com.github.gmazzo.buildconfig")
    id("maven-publish")
}

java {
    sourceCompatibility = JavaVersion.VERSION_1_8
    targetCompatibility = JavaVersion.VERSION_1_8
}

// For integration tests (TestKit): Write the plugin's classpath to a file to share with the tests.
// https://docs.gradle.org/6.0/userguide/test_kit.html#sub:test-kit-classpath-injection
val createClasspathManifest by tasks.registering {
    val outputDir = file("$buildDir/$name")

    inputs.files(sourceSets.main.get().runtimeClasspath)
        .withPropertyName("runtimeClasspath")
        .withNormalizer(ClasspathNormalizer::class)
    outputs.dir(outputDir)
        .withPropertyName("outputDir")

    doLast {
        outputDir.mkdirs()
        file("$outputDir/plugin-classpath.txt").writeText(sourceSets.main.get().runtimeClasspath.joinToString("\n"))
    }
}

val android_version: String by rootProject.extra
val kotlin_version: String by rootProject.extra
val objectbox_java_version: String by rootProject.extra
val essentials_version: String by rootProject.extra
val junit_version: String by rootProject.extra
val mockito_version: String by rootProject.extra
val truth_version: String by rootProject.extra
val moshi_version: String by rootProject.extra
val okioVersion: String by rootProject.extra

dependencies {
    implementation(gradleApi())
    implementation(project(":objectbox-code-modifier"))
    compileOnly("com.android.tools.build:gradle:$android_version")
    compileOnly("org.jetbrains.kotlin:kotlin-gradle-plugin:$kotlin_version")
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8:$kotlin_version")
    // Note: override kotlin-reflect version from com.android.tools.build:gradle to avoid mismatch with stdlib above.
    implementation("org.jetbrains.kotlin:kotlin-reflect:$kotlin_version")
    implementation("org.javassist:javassist:3.27.0-GA")

    // For Transformer to resolve class dependencies to (real) ObjectBox classes
    implementation("io.objectbox:objectbox-java:$objectbox_java_version")
    implementation("io.objectbox:objectbox-java-api:$objectbox_java_version")

    testImplementation(gradleTestKit())
    testRuntimeOnly(files(createClasspathManifest))
    testImplementation("org.greenrobot:essentials:$essentials_version")
    testImplementation("junit:junit:$junit_version")
    testImplementation("org.mockito:mockito-core:$mockito_version")
    testImplementation("com.google.truth:truth:$truth_version")
    testImplementation("com.squareup.moshi:moshi:$moshi_version")
    testImplementation("com.squareup.okio:okio:$okioVersion")
    // For plugin apply tests and outdated TestKit tests (dir "test-gradle-projects").
    testImplementation("org.jetbrains.kotlin:kotlin-gradle-plugin:$kotlin_version")
    testImplementation("com.android.tools.build:gradle:$android_version")
}

val applies_ob_java_version: String by rootProject.extra
val applies_ob_native_version: String by rootProject.extra
val applies_ob_native_sync_version: String by rootProject.extra

buildConfig {
    // rename to avoid conflict with other build config files (modules use same root package)
    className("GradlePluginBuildConfig")
    packageName("io.objectbox")

    buildConfigField("String", "VERSION", provider { "\"${project.version}\"" })
    // Versions of libraries to add to projects applying the plugin.
    buildConfigField("String", "APPLIES_JAVA_VERSION", provider { "\"$applies_ob_java_version\"" })
    buildConfigField("String", "APPLIES_NATIVE_VERSION", provider { "\"$applies_ob_native_version\"" })
    buildConfigField("String", "APPLIES_NATIVE_SYNC_VERSION", provider { "\"$applies_ob_native_sync_version\"" })
}

// For integration tests (TestKit): publish other modules to repository in build folder.
evaluationDependsOn(":objectbox-code-modifier")
evaluationDependsOn(":objectbox-generator")
evaluationDependsOn(":objectbox-processor")
publishing {
    repositories {
        maven {
            url = uri("$buildDir/repository")
            name = "test"
        }
    }
    publications {
        create<MavenPublication>("modifier") {
            val fromProject = project(":objectbox-code-modifier")
            from(fromProject.components["java"])
            groupId = fromProject.group.toString()
            artifactId = fromProject.name
            version = fromProject.version.toString()
        }
        create<MavenPublication>("generator") {
            val fromProject = project(":objectbox-generator")
            from(fromProject.components["java"])
            groupId = fromProject.group.toString()
            artifactId = fromProject.name
            version = fromProject.version.toString()
        }
        create<MavenPublication>("processor") {
            val fromProject = project(":objectbox-processor")
            from(fromProject.components["java"])
            groupId = fromProject.group.toString()
            artifactId = fromProject.name
            version = fromProject.version.toString()
        }
    }
}
tasks {
    test {
        inputs.files(
            project(":objectbox-code-modifier").tasks.named("jar"),
            project(":objectbox-generator").tasks.named("jar"),
            project(":objectbox-processor").tasks.named("jar")
        )
        dependsOn(
            "publishModifierPublicationToTestRepository",
            "publishGeneratorPublicationToTestRepository",
            "publishProcessorPublicationToTestRepository"
        )

        // For integration tests (TestKit): steal project properties required for TestKit projects.
        systemProperty("gitlabUrl", project.findProperty("gitlabUrl") ?: "")
        systemProperty("gitlabTokenName", project.findProperty("gitlabTokenName") ?: "Private-Token")
        systemProperty(
            "gitlabToken",
            project.findProperty("gitlabToken") ?: project.findProperty("gitlabPrivateToken") ?: ""
        )
    }
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
            from(components["java"])
            artifact(sourcesJar)
            artifact(javadocJar)
            pom {
                name.set("ObjectBox Gradle Plugin")
                description.set("Gradle Plugin for ObjectBox (NoSQL for Objects)")
            }
        }
    }
}
