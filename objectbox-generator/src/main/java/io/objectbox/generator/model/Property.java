/*
 * Copyright (C) 2017-2018 ObjectBox Ltd.
 *
 * This file is part of ObjectBox Build Tools.
 *
 * ObjectBox Build Tools is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * ObjectBox Build Tools is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with ObjectBox Build Tools.  If not, see <http://www.gnu.org/licenses/>.
 */

package io.objectbox.generator.model;

import java.util.LinkedHashSet;
import java.util.Set;

import javax.annotation.Nullable;

import io.objectbox.generator.IdUid;
import io.objectbox.generator.TextUtil;
import io.objectbox.model.PropertyFlags;

/** Model class for an entity's property: a Java property mapped to a data base representation. */
public class Property implements HasParsedElement {

    public static class PropertyBuilder {
        private final Property property;

        public PropertyBuilder(Schema schema, Entity entity, PropertyType propertyType, String propertyName) {
            property = new Property(schema, entity, propertyType, propertyName);
        }

        public PropertyBuilder dbName(String dbName) {
            property.dbName = dbName;
            return this;
        }

        public PropertyBuilder dbType(String dbType) {
            property.dbType = dbType;
            return this;
        }

        public PropertyBuilder modelId(IdUid modelId) {
            property.modelId = modelId;
            return this;
        }

        public PropertyBuilder modelIndexId(IdUid indexId) {
            property.modelIndexId = indexId;
            return this;
        }

        public PropertyBuilder primaryKey() {
            property.primaryKey = true;
            return this;
        }

        public PropertyBuilder notNull() {
            property.notNull = true;
            return this;
        }

        public PropertyBuilder idAssignable() {
            property.idAssignable = true;
            return this;
        }

        public PropertyBuilder fieldAccessible() {
            property.fieldAccessible = true;
            return this;
        }

        public PropertyBuilder nonPrimitiveType() {
            if (!property.propertyType.isScalar()) {
                throw new ModelRuntimeException("Type is already non-primitive");
            }
            property.nonPrimitiveType = true;
            return this;
        }

        public PropertyBuilder unsigned() {
            PropertyType type = property.propertyType;
            if (property.primaryKey || (
                    type != PropertyType.Byte && type != PropertyType.Short
                            && type != PropertyType.Int && type != PropertyType.Long
                            && type != PropertyType.Char
            )) {
                throw new RuntimeException("Only non-primary key and integer properties can be marked unsigned.");
            }
            property.isUnsigned = true;
            return this;
        }

        public PropertyBuilder index() {
            return index(PropertyFlags.INDEXED, 0, false);
        }

        public PropertyBuilder index(int type, int maxValueLength, boolean unique) {
            Index index = new Index(property);
            index.setType(type);
            index.setMaxValueLength(maxValueLength);
            if (unique) index.makeUnique();
            property.entity.addIndex(index);
            return this;
        }

        public PropertyBuilder customType(String customType, String converter) {
            property.customType = customType;
            property.customTypeClassName = TextUtil.getClassnameFromFullyQualified(customType);
            property.converter = converter;
            property.converterClassName = TextUtil.getClassnameFromFullyQualified(converter);
            return this;
        }

        public PropertyBuilder virtualTargetName(String virtualTargetName) {
            property.virtualTargetName = virtualTargetName;
            return this;
        }

        public PropertyBuilder virtualTargetValueExpression(String virtualTargetValueExpression) {
            property.virtualTargetValueExpression = virtualTargetValueExpression;
            return this;
        }

        public PropertyBuilder getterMethodName(String getterMethodName) {
            property.getterMethodName = getterMethodName;
            return this;
        }

        public Property getProperty() {
            return property;
        }
    }

    private final Schema schema;
    private final Entity entity;

    /** Only for relation ID properties, see {@link #convertToRelationId(Entity)}. */
    private Entity targetEntity;
    private PropertyType propertyType;
    private final String propertyName;

    private IdUid modelId;
    private IdUid modelIndexId;

    private String dbName;
    private String dbType;
    private Short dbTypeId;

    private String customType;
    private String customTypeClassName;
    private String converter;
    private String converterClassName;

    private boolean primaryKey;

    private boolean notNull;
    private boolean nonPrimitiveType;
    private boolean isUnsigned;
    private boolean idAssignable;
    private boolean fieldAccessible;

    private int ordinal;

    private String javaType;

    /** For virtual properties, this is target host where the property actually is located (e.g. a {@link ToOne}). */
    private String virtualTargetName;

    private String virtualTargetValueExpression;

    private String getterMethodName;

    private Object parsedElement;

    private Integer propertyFlags;
    private Set<String> propertyFlagsNames;

    /**
     * Index, which has only this property
     * Can be added by user via {@link PropertyBuilder} or via {@link Entity#addIndex(Index)}
     * Initialized in 2nd pass
     */
    private Index index;

    public Property(Schema schema, Entity entity, PropertyType propertyType, String propertyName) {
        this.schema = schema;
        this.entity = entity;
        this.propertyName = propertyName;
        this.propertyType = propertyType;
    }

    public String getPropertyName() {
        return propertyName;
    }

    public PropertyType getPropertyType() {
        return propertyType;
    }

    public IdUid getModelId() {
        return modelId;
    }

    public void setModelId(IdUid modelId) {
        this.modelId = modelId;
    }

    public IdUid getModelIndexId() {
        return modelIndexId;
    }

    public void setModelIndexId(IdUid modelIndexId) {
        this.modelIndexId = modelIndexId;
    }

    public String getDbName() {
        return dbName;
    }

    public String getDbType() {
        return dbType;
    }

    public Short getDbTypeId() {
        return dbTypeId;
    }

    public boolean isPrimaryKey() {
        return primaryKey;
    }

    public boolean isNotNull() {
        return notNull;
    }

    /** Either explicitly tagged as nonPrimitiveType OR non-scalar type (String, Date, ...) OR custom type */
    public boolean isNonPrimitiveType() {
        return nonPrimitiveType || !propertyType.isScalar() || customType != null;
    }

    public boolean isUnsigned() {
        return isUnsigned;
    }

    public boolean isIdAssignable() {
        return idAssignable;
    }

    public String getJavaType() {
        return javaType;
    }

    public String getJavaTypeInEntity() {
        if (customTypeClassName != null) {
            return customTypeClassName;
        } else {
            return javaType;
        }
    }

    public int getOrdinal() {
        return ordinal;
    }

    void setOrdinal(int ordinal) {
        this.ordinal = ordinal;
    }

    public String getCustomType() {
        return customType;
    }

    public String getCustomTypeClassName() {
        return customTypeClassName;
    }

    public String getConverter() {
        return converter;
    }

    public String getConverterClassName() {
        return converterClassName;
    }

    public boolean isVirtual() {
        return virtualTargetName != null;
    }

    public String getVirtualTargetValueExpression() {
        return virtualTargetValueExpression;
    }

    public String getVirtualTargetName() {
        return virtualTargetName;
    }

    public String getGetterMethodName() {
        return getterMethodName;
    }

    /**
     * Makes this property an relation ID - this is done after initial parsing once all entities and relations are
     * present;
     */
    void convertToRelationId(Entity target) {
        if (propertyType != PropertyType.Long && propertyType != PropertyType.RelationId) {
            throw new ModelRuntimeException("Relation ID property must be of type long: " + this);
        }
        propertyType = PropertyType.RelationId;
        targetEntity = target;
        if (index == null) {
            index = new Index(this);
            entity.addIndex(index);
        }
    }

    public Entity getTargetEntity() {
        return targetEntity;
    }

    public String getValueExpression() {
        if (fieldAccessible) {
            return propertyName;
        } else if (getterMethodName != null && getterMethodName.length() > 0) {
            return getterMethodName + "()";
        } else {
            return "get" + TextUtil.capFirst(propertyName) + "()";
        }
    }

    public String getSetValueExpression(String value) {
        if (fieldAccessible) {
            return propertyName + " = " + value;
        } else {
            return "set" + TextUtil.capFirst(propertyName) + "(" + value + ")";
        }
    }

    public String getDatabaseValueExpression() {
        return getDatabaseValueExpression(getValueExpression());
    }

    // Got too messy in template:
    // <#if property.customType?has_content>${property.propertyName}Converter.convertToDatabaseValue(</#if><#--
    // -->entity.get${property.propertyName?cap_first}()<#if property.customType?has_content>)</#if><#if
    // property.propertyType == "Boolean"> ? 1l: 0l</#if><#if property.propertyType == "Date">.getTime()</#if>
    public String getDatabaseValueExpression(String entityValue) {
        StringBuilder builder = new StringBuilder();
        if (customType != null) {
            builder.append(propertyName).append("Converter.convertToDatabaseValue(");
        }
        builder.append(entityValue);
        if (customType != null) {
            builder.append(')');
        }
        if (propertyType == PropertyType.Boolean) {
            builder.append(" ? 1 : 0");
        } else if (propertyType == PropertyType.Date) {
            builder.append(".getTime()");
        }
        return builder.toString();
    }

    public Entity getEntity() {
        return entity;
    }

    /**
     * Note: index is not set until after finishing schema.
     */
    @Nullable
    public Index getIndex() {
        return index;
    }

    public void setIndex(Index index) {
        this.index = index;
    }

    void init2ndPass() {
        if (idAssignable && !primaryKey) {
            throw new ModelRuntimeException("idSelfAssignable set for non-ID property");
        }
        if (dbType == null) {
            dbType = schema.mapToDbType(propertyType);
            dbTypeId = schema.mapToDbTypeId(propertyType);
        }
        if (dbName == null) {
            dbName = TextUtil.dbName(propertyName);
        }
        if (!nonPrimitiveType) {
            javaType = schema.mapToJavaTypeNotNull(propertyType);
        } else {
            javaType = schema.mapToJavaTypeNullable(propertyType);
        }
    }

    void init3ndPass() {
        // Nothing to do so far
    }

    public Object getParsedElement() {
        return parsedElement;
    }

    public void setParsedElement(Object parsedElement) {
        this.parsedElement = parsedElement;
    }

    /**
     * Based on this properties attributes computes required {@link PropertyFlags}.
     * @see #getPropertyFlags()
     * @see #getPropertyFlagsNames()
     */
    public void computePropertyFlags() {
        int flags = 0;
        Set<String> flagsNames = new LinkedHashSet<>(); // keep in insert-order

        if (isPrimaryKey()) {
            flags |= PropertyFlags.ID;
            flagsNames.add("PropertyFlags.ID");
        }
        if (isIdAssignable()) {
            flags |= PropertyFlags.ID_SELF_ASSIGNABLE;
            flagsNames.add("PropertyFlags.ID_SELF_ASSIGNABLE");
        }
        // Note: Primary key/ID properties must always be not null. Do not explicitly add this flag for them.
        if (isNotNull() && !isPrimaryKey()) {
            flags |= PropertyFlags.NOT_NULL;
            flagsNames.add("PropertyFlags.NOT_NULL");
        }
        if (isNonPrimitiveType() && getPropertyType().isScalar()) {
            flags |= PropertyFlags.NON_PRIMITIVE_TYPE;
            flagsNames.add("PropertyFlags.NON_PRIMITIVE_TYPE");
        }
        if (isVirtual()) {
            flags |= PropertyFlags.VIRTUAL;
            flagsNames.add("PropertyFlags.VIRTUAL");
        }
        if (isUnsigned()) {
            flags |= PropertyFlags.UNSIGNED;
            flagsNames.add("PropertyFlags.UNSIGNED");
        }

        if (getPropertyType() == PropertyType.RelationId) {
            flags |= PropertyFlags.INDEXED;
            flagsNames.add("PropertyFlags.INDEXED");
            flags |= PropertyFlags.INDEX_PARTIAL_SKIP_ZERO;
            flagsNames.add("PropertyFlags.INDEX_PARTIAL_SKIP_ZERO");
        } else if (getIndex() != null) {
            switch (getIndex().getType()) {
                case 0:
                case PropertyFlags.INDEXED:
                    flags |= PropertyFlags.INDEXED;
                    flagsNames.add("PropertyFlags.INDEXED");
                    break;
                case PropertyFlags.INDEX_HASH:
                    flags |= PropertyFlags.INDEX_HASH;
                    flagsNames.add("PropertyFlags.INDEX_HASH");
                    break;
                case PropertyFlags.INDEX_HASH64:
                    flags |= PropertyFlags.INDEX_HASH64;
                    flagsNames.add("PropertyFlags.INDEX_HASH64");
                    break;
            }
            if (getIndex().isUnique()) {
                flags |= PropertyFlags.UNIQUE;
                flagsNames.add("PropertyFlags.UNIQUE");
            }
        }

        this.propertyFlags = flags;
        this.propertyFlagsNames = flagsNames;
    }

    /**
     * Returns combined {@link io.objectbox.model.PropertyFlags} value.
     */
    public int getPropertyFlags() {
        if (propertyFlags == null) {
            computePropertyFlags();
        }
        return propertyFlags;
    }

    /**
     * Returns names of {@link io.objectbox.model.PropertyFlags}.
     */
    public Set<String> getPropertyFlagsNames() {
        if (propertyFlagsNames == null) {
            computePropertyFlags();
        }
        return propertyFlagsNames;
    }

    @Override
    public String toString() {
        return "Property " + propertyName + " of " + entity.getClassName();
    }

}
