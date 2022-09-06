plugins {
    id("java-library")
    id("java-test-fixtures")
    id("kotlin")
    id("kotlin-kapt")
    id("com.github.gmazzo.buildconfig")
}

java {
    sourceCompatibility = JavaVersion.VERSION_1_8
    targetCompatibility = JavaVersion.VERSION_1_8
}

val kotlinVersion: String by rootProject.extra
val essentialsVersion: String by rootProject.extra
val javassistVersion: String by rootProject.extra
val moshiVersion: String by rootProject.extra
val okioVersion: String by rootProject.extra
val objectboxJavaVersion: String by rootProject.extra
val junitVersion: String by rootProject.extra
val mockitoVersion: String by rootProject.extra
val truthVersion: String by rootProject.extra

dependencies {
    implementation(gradleApi())
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8:$kotlinVersion")
    implementation("org.jetbrains.kotlin:kotlin-reflect:$kotlinVersion")
    implementation("org.greenrobot:essentials:$essentialsVersion")
    implementation("org.javassist:javassist:$javassistVersion")
    implementation("com.squareup.moshi:moshi:$moshiVersion")
    kapt("com.squareup.moshi:moshi-kotlin-codegen:$moshiVersion")
    implementation("com.squareup.okio:okio:$okioVersion")

    implementation("io.objectbox:objectbox-java:$objectboxJavaVersion")
    implementation(project(":objectbox-generator"))

    testImplementation("junit:junit:$junitVersion")
    testImplementation("org.mockito:mockito-core:$mockitoVersion")
    testImplementation("com.google.truth:truth:$truthVersion")

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

apply(from = rootProject.file("gradle/objectbox-publish.gradle"))
// Set project-specific properties
configure<PublishingExtension> {
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
