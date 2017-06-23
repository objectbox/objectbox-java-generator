package io.objectbox.gradle.transform

import io.objectbox.annotation.Entity
import javassist.bytecode.SignatureAttribute

object ClassConst {
    val entityAnnotationName = Entity::class.qualifiedName

    val toOne = "io.objectbox.relation.ToOne"
    val toOneDescriptor = "Lio/objectbox/relation/ToOne;"

    val toMany = "io.objectbox.relation.ToMany"
    val toManyDescriptor = "Lio/objectbox/relation/ToMany;"

    val entityInfo = "io.objectbox.EntityInfo"
    val relationInfo = "io.objectbox.relation.RelationInfo"

    val boxStoreFieldName = "__boxStore"
    val boxStoreClass = "io.objectbox.BoxStore"

    val cursorClass = "io.objectbox.Cursor"
    val cursorAttachEntityMethodName = "attachEntity"

    val listDescriptor = "Ljava/util/List;"

    val genericSignatureT =
            SignatureAttribute.ClassSignature(arrayOf(SignatureAttribute.TypeParameter("T"))).encode()!!
}