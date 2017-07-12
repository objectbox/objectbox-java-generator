package io.objectbox.processor

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

    fun debug(message: String) {
        if (debug) {
            printMessage(Diagnostic.Kind.NOTE, message)
        }
    }

    fun info(message: String) {
        printMessage(Diagnostic.Kind.NOTE, message)
    }

    fun error(message: String) {
        printAndTrackError(message, null)
    }

    fun error(message: String, element: Element) {
        printAndTrackError(message, element)
    }

    fun error(message: String, field: VariableElement) {
        val enclosingElement = field.enclosingElement as TypeElement
        val fieldName = field.simpleName
        printAndTrackError(message + " (${enclosingElement.qualifiedName}.$fieldName)", field)
    }

    fun error(message: String, elementHolder: HasParsedElement? = null) {
        val element: Element? = if (elementHolder?.parsedElement is Element) {
            elementHolder.parsedElement as Element
        } else null
        printAndTrackError(message, element)
    }

    private fun printAndTrackError(message: String, element: Element? = null) {
        errorCount++
        printMessage(Diagnostic.Kind.ERROR, message, element)
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
