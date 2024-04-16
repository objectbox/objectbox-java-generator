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
package io.objectbox.generator.model

import io.objectbox.generator.TextUtil

/**
 * To-one relationship from a source entity to one (or zero) target entity.
 * The to-one relationship is not actually persisted, instead a property referencing the ID of the target is.
 */
class ToOne(
    val name: String,
    private val isFieldAccessible: Boolean,
    idRefPropertyName: String?,
    val idRefPropertyNameInDb: String?,
    val idRefPropertyUid: Long?,
    val targetEntityName: String
) : HasParsedElement {

    val idRefPropertyName = idRefPropertyName ?: "${name}Id"

    var idRefProperty: Property? = null
    var sourceEntity: Entity? = null
        private set
    var targetEntity: Entity? = null
        private set

    private var parsedElement: Any? = null

    @Throws(ModelException::class)
    fun setSourceAndTargetEntity(
        sourceEntity: Entity,
        targetEntity: Entity
    ) {
        this.sourceEntity = sourceEntity
        this.targetEntity = targetEntity
        idRefProperty!!.convertToRelationId(targetEntity)
    }

    // TODO This is duplicated in virtualTargetValueExpression of the refIdProperty.
    val toOneValueExpression: String
        get() = if (isFieldAccessible) name else "get" + TextUtil.capFirst(name) + "()"

    /**
     *  Checks ID reference property was converted to relation ID.
     */
    fun init3ndPass() {
        if (sourceEntity == null || targetEntity == null) {
            throw IllegalStateException("Source and target entity are not set for $this.")
        }

        val idRefProperty = idRefProperty
        if (idRefProperty != null) {
            val propertyType = idRefProperty.propertyType
            if (propertyType != PropertyType.RelationId) {
                throw IllegalStateException(
                    "To-one target ID property type is incompatible with a to-one relation: $propertyType"
                )
            }
        }
    }

    override fun getParsedElement(): Any? {
        return parsedElement
    }

    override fun setParsedElement(parsedElement: Any) {
        this.parsedElement = parsedElement
    }

    override fun toString(): String {
        val sourceName = sourceEntity?.className
        val targetName = targetEntity?.className
        return "ToOne '$name' from $sourceName to $targetName"
    }

}