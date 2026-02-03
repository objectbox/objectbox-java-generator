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
