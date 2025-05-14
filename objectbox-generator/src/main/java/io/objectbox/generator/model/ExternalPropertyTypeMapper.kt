/*
 * ObjectBox Build Tools
 * Copyright (C) 2025 ObjectBox Ltd.
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

import io.objectbox.ModelBuilder
import io.objectbox.annotation.ExternalPropertyType
import io.objectbox.model.ExternalPropertyType as ModelExternalPropertyType

object ExternalPropertyTypeMapper {

    /**
     * Maps to the string representation of a [io.objectbox.model.ExternalPropertyType] constant and returns a code
     * string that calls [ModelBuilder.PropertyBuilder.externalType] with it as parameter. For use with code generation.
     */
    @JvmStatic
    fun toExpression(externalPropertyType: ExternalPropertyType): String {
        return when (externalPropertyType) {
            ExternalPropertyType.INT_128 -> "ExternalPropertyType.Int128"
            ExternalPropertyType.UUID -> "ExternalPropertyType.Uuid"
            ExternalPropertyType.DECIMAL_128 -> "ExternalPropertyType.Decimal128"
            ExternalPropertyType.UUID_STRING -> "ExternalPropertyType.UuidString"
            ExternalPropertyType.UUID_V4 -> "ExternalPropertyType.UuidV4"
            ExternalPropertyType.UUID_V4_STRING -> "ExternalPropertyType.UuidV4String"
            ExternalPropertyType.FLEX_MAP -> "ExternalPropertyType.FlexMap"
            ExternalPropertyType.FLEX_VECTOR -> "ExternalPropertyType.FlexVector"
            ExternalPropertyType.JSON -> "ExternalPropertyType.Json"
            ExternalPropertyType.BSON -> "ExternalPropertyType.Bson"
            ExternalPropertyType.JAVASCRIPT -> "ExternalPropertyType.JavaScript"
            ExternalPropertyType.JSON_TO_NATIVE -> "ExternalPropertyType.JsonToNative"
            ExternalPropertyType.INT_128_VECTOR -> "ExternalPropertyType.Int128Vector"
            ExternalPropertyType.UUID_VECTOR -> "ExternalPropertyType.UuidVector"
            ExternalPropertyType.MONGO_ID -> "ExternalPropertyType.MongoId"
            ExternalPropertyType.MONGO_ID_VECTOR -> "ExternalPropertyType.MongoIdVector"
            ExternalPropertyType.MONGO_TIMESTAMP -> "ExternalPropertyType.MongoTimestamp"
            ExternalPropertyType.MONGO_BINARY -> "ExternalPropertyType.MongoBinary"
            ExternalPropertyType.MONGO_REGEX -> "ExternalPropertyType.MongoRegex"
        }.let { ".externalType($it)" }
    }

    /**
     * Maps to the [ModelExternalPropertyType] constant.
     */
    @JvmStatic
    fun toId(externalPropertyType: ExternalPropertyType): Short {
        return when (externalPropertyType) {
            ExternalPropertyType.INT_128 -> ModelExternalPropertyType.Int128
            ExternalPropertyType.UUID -> ModelExternalPropertyType.Uuid
            ExternalPropertyType.DECIMAL_128 -> ModelExternalPropertyType.Decimal128
            ExternalPropertyType.UUID_STRING -> ModelExternalPropertyType.UuidString
            ExternalPropertyType.UUID_V4 -> ModelExternalPropertyType.UuidV4
            ExternalPropertyType.UUID_V4_STRING -> ModelExternalPropertyType.UuidV4String
            ExternalPropertyType.FLEX_MAP -> ModelExternalPropertyType.FlexMap
            ExternalPropertyType.FLEX_VECTOR -> ModelExternalPropertyType.FlexVector
            ExternalPropertyType.JSON -> ModelExternalPropertyType.Json
            ExternalPropertyType.BSON -> ModelExternalPropertyType.Bson
            ExternalPropertyType.JAVASCRIPT -> ModelExternalPropertyType.JavaScript
            ExternalPropertyType.JSON_TO_NATIVE -> ModelExternalPropertyType.JsonToNative
            ExternalPropertyType.INT_128_VECTOR -> ModelExternalPropertyType.Int128Vector
            ExternalPropertyType.UUID_VECTOR -> ModelExternalPropertyType.UuidVector
            ExternalPropertyType.MONGO_ID -> ModelExternalPropertyType.MongoId
            ExternalPropertyType.MONGO_ID_VECTOR -> ModelExternalPropertyType.MongoIdVector
            ExternalPropertyType.MONGO_TIMESTAMP -> ModelExternalPropertyType.MongoTimestamp
            ExternalPropertyType.MONGO_BINARY -> ModelExternalPropertyType.MongoBinary
            ExternalPropertyType.MONGO_REGEX -> ModelExternalPropertyType.MongoRegex
        }
    }

}