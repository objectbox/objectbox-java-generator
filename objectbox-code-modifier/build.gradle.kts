plugins {
    id("kotlin")
    id("kotlin-kapt")
    id("com.github.gmazzo.buildconfig")
}

java {
    sourceCompatibility = JavaVersion.VERSION_1_8
    targetCompatibility = JavaVersion.VERSION_1_8
}

val kotlin_version: String by rootProject.extra
val essentials_version: String by rootProject.extra
val moshi_version: String by rootProject.extra
val okioVersion: String by rootProject.extra
val objectbox_java_version: String by rootProject.extra
val junit_version: String by rootProject.extra
val truth_version: String by rootProject.extra
val mockito_version: String by rootProject.extra

dependencies {
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8:$kotlin_version")
    implementation("org.jetbrains.kotlin:kotlin-reflect:$kotlin_version")
    implementation("org.greenrobot:essentials:$essentials_version")
    implementation("com.squareup.moshi:moshi:$moshi_version")
    kapt("com.squareup.moshi:moshi-kotlin-codegen:$moshi_version")
    implementation("com.squareup.okio:okio:$okioVersion")

    implementation("io.objectbox:objectbox-java:$objectbox_java_version")
    implementation(project(":objectbox-generator"))

    testImplementation("junit:junit:$junit_version")
    testImplementation("com.google.truth:truth:$truth_version")
    testImplementation("org.mockito:mockito-core:$mockito_version")
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
