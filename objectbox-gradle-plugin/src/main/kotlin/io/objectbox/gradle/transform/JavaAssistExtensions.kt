package io.objectbox.gradle.transform

import javassist.bytecode.FieldInfo
import javassist.bytecode.SignatureAttribute

fun FieldInfo.exGetGenericTypeArguments(): Array<out SignatureAttribute.TypeArgument>? {
    val signatureAttr = getAttribute(SignatureAttribute.tag) as SignatureAttribute
    val objectType = SignatureAttribute.toFieldSignature(signatureAttr.signature)
    return (objectType as? SignatureAttribute.ClassType)?.typeArguments
}

fun FieldInfo.exGetSingleGenericTypeArgumentOrNull(): SignatureAttribute.ClassType? {
    return exGetGenericTypeArguments()?.singleOrNull()?.type as? SignatureAttribute.ClassType
}
