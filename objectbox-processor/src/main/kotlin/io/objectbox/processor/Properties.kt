package io.objectbox.processor

import io.objectbox.annotation.Backlink
import io.objectbox.annotation.Convert
import io.objectbox.annotation.Id
import io.objectbox.annotation.Index
import io.objectbox.annotation.NameInDb
import io.objectbox.annotation.Relation
import io.objectbox.annotation.Transient
import io.objectbox.annotation.Uid
import io.objectbox.generator.IdUid
import io.objectbox.generator.model.Entity
import io.objectbox.generator.model.Property
import io.objectbox.relation.ToMany
import io.objectbox.relation.ToOne
import javax.annotation.processing.Messager
import javax.lang.model.element.AnnotationMirror
import javax.lang.model.element.Element
import javax.lang.model.element.Modifier
import javax.lang.model.element.VariableElement
import javax.lang.model.type.TypeMirror
import javax.lang.model.util.ElementFilter
import javax.lang.model.util.Elements
import javax.lang.model.util.Types

/**
 * Parses properties from fields for a given entity and adds them to the entity model.
 */
class Properties(val elementUtils: Elements, val typeUtils: Types, val messager: Messager,
                 val relations: Relations, val entityModel: Entity, entityElement: Element) {

    val fields: List<VariableElement> = ElementFilter.fieldsIn(entityElement.enclosedElements)

    fun hasBoxStoreField(): Boolean {
        return fields.find { it.simpleName.toString() == "__boxStore" } != null
    }

    fun parseFields() {
        for (field in fields) {
            parseField(field)
        }
    }

    private fun parseField(field: VariableElement) {
        // ignore static, transient or @Transient fields
        val modifiers = field.modifiers
        if (modifiers.contains(Modifier.STATIC) || modifiers.contains(Modifier.TRANSIENT)
                || field.hasAnnotation(Transient::class.java)) {
            return
        }

        if (field.hasAnnotation(Relation::class.java)) {
            // @Relation property
            relations.parseRelation(entityModel, field)
        } else if (field.asType().isTypeEqualTo(typeUtils, ToOne::class.java.name, eraseTypeParameters = true)) {
            // ToOne<TARGET> property
            relations.parseToOne(entityModel, field)
        } else if (!field.hasAnnotation(Convert::class.java)
                && field.asType().isTypeEqualTo(typeUtils, List::class.java.name, eraseTypeParameters = true)) {
            if (!field.hasAnnotation(Backlink::class.java)) {
                error("Is this a custom type or to-many relation? Add @Convert or @Backlink. (${field.qualifiedName})",
                        field)
                return
            }
            // List<TARGET> property
            relations.parseToMany(entityModel, field)
        } else if (field.asType().isTypeEqualTo(typeUtils, ToMany::class.java.name, eraseTypeParameters = true)) {
            if (!field.hasAnnotation(Backlink::class.java)) {
                error("ToMany field must be annotated with @Backlink. (${field.qualifiedName})", field)
                return
            }
            // ToMany<TARGET> property
            relations.parseToMany(entityModel, field)
        } else {
            // regular property
            parseProperty(field)
        }
    }

    private fun parseProperty(field: VariableElement) {
        // Why nullable? A property might not be parsed due to an error. We do not throw here.
        val propertyBuilder: Property.PropertyBuilder?

        if (field.hasAnnotation(Convert::class.java)) {
            // verify @Convert custom type
            propertyBuilder = parseCustomProperty(field)
        } else {
            // verify that supported type is used
            propertyBuilder = parseSupportedProperty(field)
        }
        if (propertyBuilder == null) {
            return
        }
        propertyBuilder.property.parsedElement = field

        // checks if field is accessible
        if (!field.modifiers.contains(Modifier.PRIVATE)) {
            propertyBuilder.fieldAccessible()
        }

        // @Id
        val idAnnotation = field.getAnnotation(Id::class.java)
        if (idAnnotation != null) {
            propertyBuilder.primaryKey()
            if (idAnnotation.assignable) {
                propertyBuilder.idAssignable()
            }
        }

        // @NameInDb
        val nameInDbAnnotation = field.getAnnotation(NameInDb::class.java)
        if (nameInDbAnnotation != null) {
            val name = nameInDbAnnotation.value
            if (name.isNotEmpty()) {
                propertyBuilder.dbName(name)
            }
        }

        // @Index
        if (field.hasAnnotation(Index::class.java)) {
            propertyBuilder.indexAsc(null, false)
        }

        // @Uid
        val uidAnnotation = field.getAnnotation(Uid::class.java)
        if (uidAnnotation != null && uidAnnotation.value != 0L) {
            // just storing uid, id model sync will replace with correct id+uid
            propertyBuilder.modelId(IdUid(0, uidAnnotation.value))
        }
    }

    private fun parseCustomProperty(field: VariableElement): Property.PropertyBuilder? {
        // extract @Convert annotation member values
        // as they are types, need to access them via annotation mirrors
        val annotationMirror = getAnnotationMirror(field, Convert::class.java) ?: return null // did not find @Convert mirror

        // converter and dbType value existence guaranteed by compiler
        val converter = getAnnotationValueType(annotationMirror, "converter")
        val dbType = getAnnotationValueType(annotationMirror, "dbType")

        val propertyType = dbType?.getPropertyType(typeUtils)
        if (propertyType == null) {
            error("@Convert dbType type is not supported, use a Java primitive wrapper class. (${field.qualifiedName})",
                    field)
            return null
        }

        // may be a parameterized type like List<CustomType>, so erase any type parameters
        val customType = typeUtils.erasure(field.asType())

        val propertyBuilder = entityModel.addProperty(propertyType, field.simpleName.toString())
        propertyBuilder.customType(customType.toString(), converter.toString())
        // note: custom types are already assumed non-primitive by Property#isNonPrimitiveType()
        return propertyBuilder
    }

    private fun parseSupportedProperty(field: VariableElement): Property.PropertyBuilder? {
        val typeMirror = field.asType()
        val propertyType = typeMirror.getPropertyType(typeUtils)
        if (propertyType == null) {
            error("Field type is not supported, use @Convert or @Transient. (${field.qualifiedName})", field)
            return null
        }

        val propertyBuilder = entityModel.addProperty(propertyType, field.simpleName.toString())

        val isPrimitive = typeMirror.kind.isPrimitive
        if (isPrimitive) {
            // treat primitive types as non-null
            propertyBuilder.notNull()
        } else if (propertyType.isScalar) {
            // treat wrapper types (Long, Integer, ...) of scalar types as non-primitive
            propertyBuilder.nonPrimitiveType()
        }

        return propertyBuilder
    }

    private fun getAnnotationMirror(element: Element, annotationClass: Class<*>): AnnotationMirror? {
        val annotationMirrors = element.annotationMirrors
        for (annotationMirror in annotationMirrors) {
            val annotationType = annotationMirror.annotationType
            val convertType = elementUtils.getTypeElement(annotationClass.canonicalName).asType()
            if (typeUtils.isSameType(annotationType, convertType)) {
                return annotationMirror
            }
        }
        return null
    }

    private fun getAnnotationValueType(annotationMirror: AnnotationMirror, memberName: String): TypeMirror? {
        val elementValues = annotationMirror.elementValues
        for ((key, value) in elementValues) {
            val elementName = key.simpleName.toString()
            if (elementName == memberName) {
                // this is a shortcut instead of using entry.getValue().accept(visitor, null)
                return value.value as TypeMirror
            }
        }
        return null
    }

    private fun error(message: String, element: Element? = null) {
        messager.printCustomError(message, element)
    }

}