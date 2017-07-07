package io.objectbox.gradle.transform

import javassist.Modifier
import javassist.bytecode.AccessFlag
import javassist.bytecode.AnnotationsAttribute
import javassist.bytecode.ClassFile
import javassist.bytecode.FieldInfo
import javassist.bytecode.SignatureAttribute
import javassist.bytecode.annotation.Annotation

fun FieldInfo.exGetGenericTypeArguments(): Array<out SignatureAttribute.TypeArgument>? {
    val signatureAttr = getAttribute(SignatureAttribute.tag) as SignatureAttribute
    val objectType = SignatureAttribute.toFieldSignature(signatureAttr.signature)
    return (objectType as? SignatureAttribute.ClassType)?.typeArguments
}

fun FieldInfo.exGetSingleGenericTypeArgumentOrNull(): SignatureAttribute.ClassType? {
    return exGetGenericTypeArguments()?.singleOrNull()?.type as? SignatureAttribute.ClassType
}

fun FieldInfo.exGetModifiers(): Int {
    return AccessFlag.toModifier(accessFlags)
}

fun FieldInfo.exIsTransient(): Boolean {
    return Modifier.isTransient(exGetModifiers())
}

fun FieldInfo.exGetAnnotation(name: String): Annotation? {
    var annotationsAttribute = getAttribute(AnnotationsAttribute.visibleTag) as AnnotationsAttribute?
    var annotation = annotationsAttribute?.getAnnotation(name)
    if (annotation == null) {
        annotationsAttribute = getAttribute(AnnotationsAttribute.invisibleTag) as AnnotationsAttribute?
        annotation = annotationsAttribute?.getAnnotation(name)
    }
    return annotation
}

fun ClassFile.exGetAnnotation(name: String): Annotation? {
    var annotationsAttribute = getAttribute(AnnotationsAttribute.visibleTag) as AnnotationsAttribute?
    var annotation = annotationsAttribute?.getAnnotation(name)
    if (annotation == null) {
        annotationsAttribute = getAttribute(AnnotationsAttribute.invisibleTag) as AnnotationsAttribute?
        annotation = annotationsAttribute?.getAnnotation(name)
    }
    return annotation
}
