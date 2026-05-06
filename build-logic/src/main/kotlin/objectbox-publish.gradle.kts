// This convention plugin expects some Gradle project properties to be set
// (to set as environment variable prefix with ORG_GRADLE_PROJECT_):
// https://docs.gradle.org/current/userguide/build_environment.html#sec:project_properties
//
// To publish artifacts to the internal GitLab repo set:
// - gitlabUrl
// - gitlabToken or gitlabPrivateToken: a token with permission to publish to the GitLab Package Repository
// - gitlabTokenName: optional, if set used instead of "Private-Token". Use for CI to specify e.g. "Job-Token".
//
// To sign artifacts using an ASCII encoded PGP key given via a file set:
// - signingKeyFile
// - signingKeyId
// - signingPassword

// Gradle properties
val gitlabUrl = providers.gradleProperty("gitlabUrl")
val gitlabTokenName = providers.gradleProperty("gitlabTokenName")
val gitlabToken = providers.gradleProperty("gitlabToken")
val gitlabPrivateToken = providers.gradleProperty("gitlabPrivateToken")
// The following properties are used for signing in CI using a key file
val signingKeyFile = providers.gradleProperty("signingKeyFile")
val signingKeyId = providers.gradleProperty("signingKeyId")
val signingPassword = providers.gradleProperty("signingPassword")

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
        // If the applied to project has the required properties, configures the "GitLab" repository for publishing
        // (Note: always adding it, even without credentials, so it's possible to see the tasks created for it.)
        maven {
            name = "GitLab"
            if (gitlabUrl.isPresent && (gitlabToken.isPresent || gitlabPrivateToken.isPresent)) {
                // "https://gitlab.example.com/api/v4/projects/<PROJECT_ID>/packages/maven"
                url = uri("${gitlabUrl.get()}/api/v4/projects/18/packages/maven")
                credentials(HttpHeaderCredentials::class) {
                    name = gitlabTokenName.orNull ?: "Private-Token"
                    value = gitlabToken.orNull ?: gitlabPrivateToken.get()
                }
                authentication {
                    create<HttpHeaderAuthentication>("header")
                }
                println("Publishing: configured GitLab repository $url")
            } else {
                println("Publishing: GitLab repository NOT configured, see objectbox-publish plugin for required project properties")
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
    if (signingKeyFile.isPresent && signingKeyId.isPresent && signingPassword.isPresent) {
        // Sign using an ASCII-armored key read from a file
        // https://docs.gradle.org/current/userguide/signing_plugin.html#using_in_memory_ascii_armored_openpgp_subkeys
        val keyFilePath = signingKeyFile.get()
        val signingKey = File(keyFilePath).readText()
        useInMemoryPgpKeys(signingKeyId.get(), signingKey, signingPassword.get())
        sign(publishing.publications)
        println("Publishing: signing configured with key file $keyFilePath")
    } else {
        isRequired = false // Don't run sign tasks
        println("Publishing: signing NOT configured, see objectbox-publish plugin for required project properties")
    }
}
