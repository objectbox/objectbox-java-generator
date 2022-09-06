plugins {
    id("java")
    id("kotlin")
    id("objectbox-publish")
}

java {
    sourceCompatibility = JavaVersion.VERSION_1_8
    targetCompatibility = JavaVersion.VERSION_1_8
}

val kotlinVersion: String by rootProject.extra
val objectboxJavaVersion: String by rootProject.extra
val essentialsVersion: String by rootProject.extra
val junitVersion: String by rootProject.extra

dependencies {
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8:$kotlinVersion")
    implementation("org.jetbrains.kotlin:kotlin-reflect:$kotlinVersion")

    implementation("io.objectbox:objectbox-java:$objectboxJavaVersion")
    // https://freemarker.apache.org/docs/app_versions.html
    implementation("org.freemarker:freemarker:2.3.31")
    implementation("org.greenrobot:essentials:$essentialsVersion")

    testImplementation("junit:junit:$junitVersion")
}

tasks.test {
    doFirst {
        mkdir("test-out")
    }
    doLast {
        delete("test-out")
    }
}

tasks.javadoc {
    isFailOnError = false
    title = "ObjectBox Generator $version API"
    (options as StandardJavadocDocletOptions).bottom = /*"Available under the GPLv3 - */"<i>Copyright &#169; 2022 <a href=\"https://objectbox.io/\">ObjectBox Ltd</a>. All Rights Reserved.</i>"
    doLast {
        copy {
            from("../javadoc-style/")
            into("build/docs/javadoc/")
        }
    }
}

val javadocJar by tasks.registering(Jar::class) {
    dependsOn(tasks.javadoc)
    archiveClassifier.set("javadoc")
    from("build/docs/javadoc")
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
            artifact(javadocJar)
            pom {
                name.set("ObjectBox Generator")
                description.set("Code generator for ObjectBox, the superfast NoSQL database for Objects")
            }
        }
    }
}
