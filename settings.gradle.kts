pluginManagement {
    repositories {
        // Google: for Android plugin
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral() // For dokka plugin
        gradlePluginPortal()
    }
}

// Note: when changing, also update project setup in GradleTestRunner as needed.
// While this is an incubating API, it is the recommended way of declaring repositories:
// https://docs.gradle.org/current/userguide/best_practices_dependencies.html#set_up_repositories_in_settings
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google() // For Android dependencies
        mavenCentral()

        // Internal ObjectBox repo to get snapshot versions of dependencies
        val gitlabUrl = providers.gradleProperty("gitlabUrl")
        val gitlabTokenName = providers.gradleProperty("gitlabTokenName")
        val gitlabToken = providers.gradleProperty("gitlabToken")
        val gitlabPrivateToken = providers.gradleProperty("gitlabPrivateToken")
        if (gitlabUrl.isPresent) {
            maven {
                name = "GitLab"
                url = uri("${gitlabUrl.get()}/api/v4/groups/objectbox/-/packages/maven")
                credentials(HttpHeaderCredentials::class) {
                    name = gitlabTokenName.orNull ?: "Private-Token"
                    value = gitlabToken.orNull ?: gitlabPrivateToken.get()
                }
                authentication {
                    create<HttpHeaderAuthentication>("header")
                }
                println("Dependencies: added GitLab repository at $url")
            }
        } else {
            println("Dependencies: GitLab repository NOT added, see settings file for required project properties")
        }
        
        mavenLocal()
    }
}

plugins {
    // Projects use toolchain to compile with specific Java language version, add plugin to enable JDK auto-download.
    // https://docs.gradle.org/current/userguide/toolchains.html#sec:provisioning
    id("org.gradle.toolchains.foojay-resolver-convention") version("0.7.0")
}

include(":objectbox-generator")
include(":objectbox-code-modifier")
include(":objectbox-gradle-plugin")
include(":objectbox-processor")
include(":agp-wrapper-7-2")
