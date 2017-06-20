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
class Messages(val messager: Messager) {

    var errorCount: Int = 0

    val errorRaised: Boolean
        get() = errorCount > 0

    fun error(message: String) {
        printCustomError(message, null)
    }

    fun error(message: String, element: Element) {
        printCustomError(message, element)
    }

    fun error(message: String, field: VariableElement) {
        val enclosingElement = field.enclosingElement as TypeElement
        val fieldName = field.simpleName
        printCustomError(message + " (${enclosingElement.qualifiedName}.$fieldName)", field)
    }

    fun error(message: String, elementHolder: HasParsedElement? = null) {
        val element: Element? = if (elementHolder?.parsedElement is Element) {
            elementHolder.parsedElement as Element
        } else null
        printCustomError(message, element)
    }

    private fun printCustomError(message: String, element: Element? = null) {
        errorCount++
        if (element != null) {
            messager.printMessage(Diagnostic.Kind.ERROR, message, element)
        } else {
            messager.printMessage(Diagnostic.Kind.ERROR, "ObjectBox: " + message)
        }
    }

}
