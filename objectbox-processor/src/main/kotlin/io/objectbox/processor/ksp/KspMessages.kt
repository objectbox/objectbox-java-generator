/*
 * ObjectBox Build Tools
 * Copyright (C) 2026 ObjectBox Ltd.
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

package io.objectbox.processor.ksp

import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.symbol.KSNode
import io.objectbox.generator.model.HasParsedElement
import io.objectbox.processor.CommonMessages
import io.objectbox.processor.ProcessingClass
import io.objectbox.processor.ProcessingField
import io.objectbox.reporting.BasicBuildTracker

/**
 * Helps raise and keep count of errors during processing, like [io.objectbox.processor.Messages],
 * but for the KSP processor.
 */
class KspMessages(private val logger: KSPLogger, private val debug: Boolean) : CommonMessages {

    private var errorCount: Int = 0

    override val errorRaised: Boolean
        get() = errorCount > 0

    /**
     * If debug mode is enabled, prints `message`. Otherwise does nothing.
     */
    override fun debug(message: String) {
        if (debug) {
            printMessage(message, null, isError = false)
        }
    }

    /**
     * Prints `message` as info.
     */
    override fun info(message: String) {
        printMessage(message, null, isError = false)
    }

    /**
     * Prints `message` as error.
     */
    override fun error(message: String) {
        printAndTrackError(message, null)
    }

    /**
     * Prints `message` as error, links to `node`.
     */
    fun error(message: String, node: KSNode) {
        printAndTrackError(message, node)
    }

    override fun error(message: String, linkToClass: ProcessingClass) {
        printAndTrackError(message, linkToClass.element as? KSNode)
    }

    override fun error(message: String, field: ProcessingField) {
        printAndTrackError("$message (${field.enclosingQualifiedName}.${field.name})", field.element as? KSNode)
    }

    /**
     * Prints `message` as error, links to `elementHolder.parsedElement` if it is a [KSNode].
     */
    override fun error(message: String, elementHolder: HasParsedElement?) {
        // The parsed element is either a node of this processor API or a ProcessingField.
        val parsedElement = (elementHolder?.parsedElement as? ProcessingField)?.element
            ?: elementHolder?.parsedElement
        printAndTrackError(message, parsedElement as? KSNode)
    }

    private fun printAndTrackError(message: String, node: KSNode? = null) {
        errorCount++
        printMessage(message, node, isError = true)
        val buildTracker = BasicBuildTracker("Processor")
        buildTracker.disconnect = false
        buildTracker.trackError(message)
    }

    private fun printMessage(message: String, node: KSNode?, isError: Boolean) {
        val prefixedMessage = "[ObjectBox] $message"
        if (debug) {
            // This is only visible in tests:
            println(prefixedMessage)
        }
        if (isError) {
            logger.error(prefixedMessage, node)
        } else {
            logger.info(prefixedMessage, node)
        }
    }

}
