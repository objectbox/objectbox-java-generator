/*
 * ObjectBox Build Tools
 * Copyright (C) 2021-2024 ObjectBox Ltd.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published
 * by the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

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
