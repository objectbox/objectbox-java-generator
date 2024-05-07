// Configures common publishing settings

plugins {
    id("maven-publish")
    id("signing")
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
                println("GitLab repository set to $url.")

                credentials(HttpHeaderCredentials::class) {
                    name = project.findProperty("gitlabTokenName")?.toString()
                        ?: "Private-Token"
                    value = project.findProperty("gitlabToken")?.toString()
                        ?: project.property("gitlabPrivateToken").toString()
                }
                authentication {
                    create<HttpHeaderAuthentication>("header")
                }
            } else {
                println("WARNING: Can not publish to GitLab: gitlabUrl or gitlabToken/gitlabPrivateToken not set.")
            }
        }
        // Note: Sonatype repo created by publish-plugin, see root build.gradle.kts.
    }

    publications {
        create<MavenPublication>("mavenJava") {
            // Note: Projects set additional specific properties.
            pom {
                packaging = "jar"
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
    if (hasSigningProperties()) {
        val signingKey = File(project.property("signingKeyFile").toString()).readText()
        useInMemoryPgpKeys(
            project.property("signingKeyId").toString(),
            signingKey,
            project.property("signingPassword").toString()
        )
        sign(publishing.publications["mavenJava"])
    } else {
        println("WARNING: Signing information missing/incomplete for ${project.name}")
    }
}

fun hasSigningProperties(): Boolean {
    return (project.hasProperty("signingKeyId")
            && project.hasProperty("signingKeyFile")
            && project.hasProperty("signingPassword"))
}
