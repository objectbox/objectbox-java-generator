/*
 * Copyright (C) 2017-2018 ObjectBox Ltd.
 *
 * This file is part of ObjectBox Build Tools.
 *
 * ObjectBox Build Tools is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * ObjectBox Build Tools is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with ObjectBox Build Tools.  If not, see <http://www.gnu.org/licenses/>.
 */

package io.objectbox.processor

import io.objectbox.reporting.BasicBuildTracker
import io.objectbox.generator.model.HasParsedElement
import javax.annotation.processing.Messager
import javax.lang.model.element.Element
import javax.lang.model.element.TypeElement
import javax.lang.model.element.VariableElement
import javax.tools.Diagnostic

/**
 * Helps raise and keep count of errors during processing.
 */
class Messages(val messager: Messager, val debug: Boolean) {

    var errorCount: Int = 0

    val errorRaised: Boolean
        get() = errorCount > 0

    /**
     * If debug mode is enabled, prints `message` as note. Otherwise does nothing.
     */
    fun debug(message: String) {
        if (debug) {
            printMessage(Diagnostic.Kind.NOTE, message)
        }
    }

    /**
     * Prints `message` as note.
     */
    fun info(message: String) {
        printMessage(Diagnostic.Kind.NOTE, message)
    }

    /**
     * Prints `message` as error.
     */
    fun error(message: String) {
        printAndTrackError(message, null)
    }

    /**
     * Prints `message` as error, links to `element`.
     */
    fun error(message: String, element: Element) {
        printAndTrackError(message, element)
    }

    /**
     * Prints error like `message (<qualified name>)`, links to `field`.
     */
    fun error(message: String, field: VariableElement) {
        val enclosingElement = field.enclosingElement as TypeElement
        val fieldName = field.simpleName
        printAndTrackError(message + " (${enclosingElement.qualifiedName}.$fieldName)", field)
    }

    /**
     * Prints `message` as error, links to `elementHolder.parsedElement` if not null.
     */
    fun error(message: String, elementHolder: HasParsedElement? = null) {
        val element: Element? = if (elementHolder?.parsedElement is Element) {
            elementHolder.parsedElement as Element
        } else null
        printAndTrackError(message, element)
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
