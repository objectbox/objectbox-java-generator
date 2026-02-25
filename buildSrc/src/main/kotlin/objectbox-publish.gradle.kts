// Configures common publishing settings

plugins {
    id("maven-publish")
    id("signing")
}

/**
 * Creates a javadoc JAR with a helpful README.md as there aren't really APIs to document.
 *
 * This satisfies
 * [Maven Central requirements](https://central.sonatype.org/publish/requirements/#supply-javadoc-and-sources).
 *
 * Add to a publication like:
 *
 * ```
 * publishing {
 *     publications {
 *         create<MavenPublication>("obxProject") {
 *             artifact(tasks.named("javadocReadmeJar"))
 *         }
 *     }
 * }
 * ```
 */
val javadocReadmeJar by tasks.registering(Jar::class) {
    archiveClassifier.set("javadoc")
    from(rootProject.file("javadoc/README.md"))
}

publishing {
    repositories {
        maven {
            name = "GitLab"
            if (project.hasProperty("gitlabUrl")
                && (project.hasProperty("gitlabToken") || project.hasProperty("gitlabPrivateToken"))
            ) {
                // "https://gitlab.example.com/api/v4/projects/<PROJECT_ID>/packages/maven"
                val gitlabUrl = project.property("gitlabUrl")
                url = uri("$gitlabUrl/api/v4/projects/18/packages/maven")
                credentials(HttpHeaderCredentials::class) {
                    name = project.findProperty("gitlabTokenName")?.toString()
                        ?: "Private-Token"
                    value = project.findProperty("gitlabToken")?.toString()
                        ?: project.property("gitlabPrivateToken").toString()
                }
                authentication {
                    create<HttpHeaderAuthentication>("header")
                }
                println("Publishing: configured GitLab repository $url")
            } else {
                println("Publishing: GitLab repository not configured")
            }
        }
        // Note: Sonatype repo created by publish-plugin, see root build.gradle.kts.
    }

    publications {
        // Common configuration for all Maven publications
        withType<MavenPublication> {
            pom {
                url.set("https://objectbox.io")
                licenses {
                    license {
                        name.set("GNU Affero General Public License, Version 3")
                        url.set("https://www.gnu.org/licenses/agpl-3.0.html")
                        distribution.set("repo")
                    }
                }
                developers {
                    developer {
                        id.set("ObjectBox")
                        name.set("ObjectBox")
                    }
                }
                issueManagement {
                    system.set("GitHub Issues")
                    url.set("https://github.com/objectbox/objectbox-java-generator/issues")
                }
                organization {
                    name.set("ObjectBox Ltd.")
                    url.set("https://objectbox.io")
                }
                scm {
                    connection.set("scm:git@github.com:objectbox/objectbox-java-generator.git")
                    developerConnection.set("scm:git@github.com:objectbox/objectbox-java-generator.git")
                    url.set("https://github.com/objectbox/objectbox-java-generator")
                }
            }
        }
    }
}

signing {
    // Sign all publications
    if (hasSigningProperties()) {
        val signingKey = File(project.property("signingKeyFile").toString()).readText()
        useInMemoryPgpKeys(
            project.property("signingKeyId").toString(),
            signingKey,
            project.property("signingPassword").toString()
        )
        sign(publishing.publications)
        println("Publishing: configured signing with key file")
    } else {
        println("Publishing: signing not configured")
    }
}

fun hasSigningProperties(): Boolean {
    return (project.hasProperty("signingKeyId")
            && project.hasProperty("signingKeyFile")
            && project.hasProperty("signingPassword"))
}
