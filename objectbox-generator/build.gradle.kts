plugins {
    id("java")
    kotlin("jvm")
    kotlin("kapt")
    id("objectbox-publish")
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(8))
    }
    withJavadocJar()
}

val objectboxJavaVersion: String by rootProject.extra
val essentialsVersion: String by rootProject.extra
val moshiVersion: String by rootProject.extra
val okioVersion: String by rootProject.extra
val junitVersion: String by rootProject.extra
val truthVersion: String by rootProject.extra

dependencies {
    // Note: Kotlin plugin adds kotlin-stdlib-jdk8 dependency.

    implementation("io.objectbox:objectbox-java:$objectboxJavaVersion")
    // https://freemarker.apache.org/docs/app_versions.html
    implementation("org.freemarker:freemarker:2.3.34")
    implementation("org.greenrobot:essentials:$essentialsVersion")
    implementation("com.squareup.moshi:moshi:$moshiVersion")
    kapt("com.squareup.moshi:moshi-kotlin-codegen:$moshiVersion")
    implementation("com.squareup.okio:okio:$okioVersion")

    testImplementation("junit:junit:$junitVersion")
    testImplementation("com.google.truth:truth:$truthVersion")
}

tasks.test {
    doFirst {
        mkdir("test-out")
    }
    doLast {
        delete("test-out")
    }
}

val sourcesJar by tasks.registering(Jar::class) {
    archiveClassifier.set("sources")
    from("README")
}

// Set project-specific properties
publishing {
    publications {
        getByName<MavenPublication>("mavenJava") {
            artifactId = "objectbox-generator"
            from(components["java"])
            artifact(sourcesJar)
            pom {
                name.set("ObjectBox Generator")
                description.set("Code generator for ObjectBox, the superfast NoSQL database for Objects")
            }
        }
    }
}
