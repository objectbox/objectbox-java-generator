@file:JvmName("Logging")

package io.objectbox.logging


/**
 * Creates a Gradle log message pre-fixed with "`[ObjectBox]`".
 */
fun log(message: String) {
    // Gradle collects standard output as log message with level QUIET (visible by default).
    // https://docs.gradle.org/current/userguide/logging.html
    println("[ObjectBox] $message")
}

/**
 * Creates a Gradle log message that is highlighted as a warning in IntelliJ and Android Studio.
 */
fun logWarning(message: String) {
    // IntelliJ and Android Studio highlight log messages prefixed with WARNING.
    log("WARNING: [ObjectBox] $message")
}
