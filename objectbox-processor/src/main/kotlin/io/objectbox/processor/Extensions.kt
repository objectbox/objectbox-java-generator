package io.objectbox.processor

import javax.annotation.processing.Messager
import javax.lang.model.element.Element
import javax.lang.model.element.TypeElement
import javax.lang.model.element.VariableElement
import javax.tools.Diagnostic

val VariableElement.qualifiedName: String
    get() {
        val enclosingElement = enclosingElement as TypeElement
        val fieldName = simpleName
        return "${enclosingElement.qualifiedName}.$fieldName"
    }

fun <A : Annotation> Element.hasAnnotation(annotationType: Class<A>): Boolean {
    return getAnnotation(annotationType) != null
}

fun Messager.printCustomError(message: String, element: Element? = null) {
    if (element != null) {
        printMessage(Diagnostic.Kind.ERROR, message, element)
    } else {
        printMessage(Diagnostic.Kind.ERROR, "ObjectBox: " + message)
    }
}
