/*
 * ObjectBox Build Tools
 * Copyright (C) 2017-2024 ObjectBox Ltd.
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

package io.objectbox.processor

import io.objectbox.reporting.BasicBuildTracker
import io.objectbox.generator.model.HasParsedElement
import javax.annotation.processing.Messager
import javax.lang.model.element.Element
import javax.tools.Diagnostic

/**
 * Helps raise and keep count of errors during processing.
 */
class Messages(private val messager: Messager, private val debug: Boolean) : CommonMessages {

    private var errorCount: Int = 0

    override val errorRaised: Boolean
        get() = errorCount > 0

    /**
     * If debug mode is enabled, prints `message` as note. Otherwise does nothing.
     */
    override fun debug(message: String) {
        if (debug) {
            printMessage(Diagnostic.Kind.NOTE, message)
        }
    }

    /**
     * Prints `message` as note.
     */
    override fun info(message: String) {
        printMessage(Diagnostic.Kind.NOTE, message)
    }

    /**
     * Prints `message` as error.
     */
    override fun error(message: String) {
        printAndTrackError(message, null)
    }

    /**
     * Prints `message` as error, links to `elementHolder.parsedElement` if not null.
     */
    override fun error(message: String, elementHolder: HasParsedElement?) {
        // The parsed element is either an element of this processor API or a ProcessingField.
        val parsedElement = (elementHolder?.parsedElement as? ProcessingField)?.element
            ?: elementHolder?.parsedElement
        printAndTrackError(message, parsedElement as? Element)
    }

    override fun error(message: String, linkToClass: ProcessingClass) {
        printAndTrackError(message, linkToClass.element as? Element)
    }

    /**
     * Prints error like `message (<qualified name>.<field name>)`, links to the field.
     */
    override fun error(message: String, field: ProcessingField) {
        printAndTrackError(message + " (${field.enclosingQualifiedName}.${field.name})", field.element as? Element)
    }

    private fun printAndTrackError(message: String, element: Element? = null) {
        errorCount++
        printMessage(Diagnostic.Kind.ERROR, message, element)
        val buildTracker = BasicBuildTracker("Processor")
        buildTracker.disconnect = false
        buildTracker.trackError(message)
    }

    private fun printMessage(kind: Diagnostic.Kind, message: String, element: Element? = null) {
        val prefixedMessage = "[ObjectBox] $message"
        if (debug) {
            // This is only visible in tests:
            println(prefixedMessage)
        }
        messager.printMessage(kind, prefixedMessage, element)
    }

}
