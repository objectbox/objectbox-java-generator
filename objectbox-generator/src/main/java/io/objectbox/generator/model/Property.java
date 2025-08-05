/*
 * ObjectBox Build Tools
 * Copyright (C) 2017-2025 ObjectBox Ltd.
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

package io.objectbox.generator.model;

import java.util.LinkedHashSet;
import java.util.Set;

import javax.annotation.Nullable;

import io.objectbox.annotation.ExternalPropertyType;
import io.objectbox.annotation.HnswIndex;
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

        /**
         * @see Property#getExternalName()
         */
        public PropertyBuilder externalName(String externalName) {
            property.externalName = externalName;
            return this;
        }

        /**
         * @see Property#getExternalTypeId()
         * @see Property#getExternalTypeExpression()
         */
        public PropertyBuilder externalType(ExternalPropertyType externalType) {
            property.externalTypeId = ExternalPropertyTypeMapper.toId(externalType);
            property.externalTypeExpression = ExternalPropertyTypeMapper.toExpression(externalType);
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

        public PropertyBuilder idCompanion() {
            if (property.propertyType != PropertyType.Date && property.propertyType != PropertyType.DateNano) {
                throw new RuntimeException("Only Date or DateNano properties can be an ID companion.");
            }
            property.idCompanion = true;
            return this;
        }

        /**
         * @see Property#isTypeNotNull()
         */
        public PropertyBuilder typeNotNull() {
            property.isTypeNotNull = true;
            return this;
        }

        /**
         * @see Property#isNotNullFlag()
         */
        @SuppressWarnings("unused") // Currently not used.
        public PropertyBuilder notNullFlag() {
            property.isNotNullFlag = true;
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

        /**
         * See {@link Property#isNonPrimitiveFlag()}.
         */
        public PropertyBuilder nonPrimitiveFlag() {
            if (!property.propertyType.isScalar()
                    && property.customType == null
                    && property.propertyType != PropertyType.StringArray) {
                throw new ModelRuntimeException("Type is already non-primitive");
            }
            property.isNonPrimitiveFlag = true;
            return this;
        }

        /**
         * See {@link Property#isList()}.
         */
        public PropertyBuilder isList() {
            if (property.propertyType != PropertyType.StringArray) {
                throw new ModelRuntimeException("Setting isList currently only supported for StringArray properties.");
            }
            property.isList = true;
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
            return index(PropertyFlags.INDEXED, 0);
        }

        /**
         * @param indexFlags One or more of the INDEX or UNIQUE {@link io.objectbox.model.PropertyFlags}.
         */
        public PropertyBuilder index(int indexFlags, int maxValueLength) {
            Index index = new Index(property, indexFlags);
            index.setMaxValueLength(maxValueLength);
            property.entity.addIndex(index);
            return this;
        }

        /**
         * Sets HNSW index parameters, implicitly creates an {@link #index()}.
         *
         * @param annotation to extract parameters from.
         * @return this builder.
         * @throws ModelException if invalid parameters are set.
         */
        public PropertyBuilder hnswParams(HnswIndex annotation) throws ModelException {
            index();
            property.hnswParams = HnswParams.fromAnnotation(annotation);
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

    @Nullable
    private String externalName;
    @Nullable
    private Short externalTypeId;
    @Nullable
    private String externalTypeExpression;

    private String customType;
    private String customTypeClassName;
    private String converter;
    private String converterClassName;

    private boolean primaryKey;
    private boolean idCompanion;

    private boolean isTypeNotNull;
    private boolean isNotNullFlag;
    private boolean isNonPrimitiveFlag;
    private boolean isList;
    private boolean isUnsigned;
    private boolean idAssignable;
    private boolean fieldAccessible;

    private int ordinal;

    private String javaType;
    @Nullable
    private String javaRawType;

    /** For virtual properties, this is target host where the property actually is located (e.g. a {@link ToOne}). */
    private String virtualTargetName;

    private String virtualTargetValueExpression;

    private String getterMethodName;

    private Object parsedElement;

    private Integer propertyFlagsModelFile;
    private Set<String> propertyFlagsGeneratedCode;

    /**
     * Index, which has only this property
     * Can be added by user via {@link PropertyBuilder} or via {@link Entity#addIndex(Index)}
     * Initialized in 2nd pass
     */
    private Index index;

    @Nullable
    private HnswParams hnswParams;

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

    /**
     * String representation of a {@link PropertyType} enum.
     */
    public String getDbType() {
        return dbType;
    }

    /**
     * ID of the {@link io.objectbox.model.PropertyType}.
     */
    public Short getDbTypeId() {
        return dbTypeId;
    }

    /**
     * The {@link io.objectbox.annotation.ExternalName} of this property.
     */
    @Nullable
    public String getExternalName() {
        return externalName;
    }

    /**
     * The ID of the {@link io.objectbox.annotation.ExternalType} of this property.
     * <p>
     * See {@link ExternalPropertyTypeMapper#toId(ExternalPropertyType)}.
     */
    @Nullable
    public Short getExternalTypeId() {
        return externalTypeId;
    }

    /**
     * The code expression to use in generated {@link io.objectbox.ModelBuilder} code that sets the
     * {@link io.objectbox.annotation.ExternalType} of this property.
     * <p>
     * See {@link ExternalPropertyTypeMapper#toExpression(ExternalPropertyType)}.
     */
    @Nullable
    public String getExternalTypeExpression() {
        return externalTypeExpression;
    }

    /**
     * If this property is a primary key and {@link PropertyFlags#ID} should be set.
     */
    public boolean isPrimaryKey() {
        return primaryKey;
    }

    /**
     * If {@link PropertyFlags#ID_COMPANION} should be set on this property.
     */
    public boolean isIdCompanion() {
        return idCompanion;
    }

    /**
     * If a property value may never be null, so e.g. null checks in generated code are not necessary.
     * <p>
     * Note: this is not related to {@link PropertyFlags#NOT_NULL}, use {@link #isNotNullFlag()} instead.
     */
    public boolean isTypeNotNull() {
        return isTypeNotNull;
    }

    /**
     * If {@link PropertyFlags#NOT_NULL} should be set on this property.
     * <p>
     * Note: this does not indicate if the property value may never be null,
     * use {@link #isTypeNotNull()} instead.
     */
    public boolean isNotNullFlag() {
        return isNotNullFlag;
    }

    /**
     * If {@link PropertyFlags#NON_PRIMITIVE_TYPE} should be set on this property.
     * <p>
     * This will indicate to the database that either
     * <ul>
     * <li>a wrapper type should be used for a scalar property (e.g. Long instead of long),</li>
     * <li>a custom type is used, or</li>
     * <li>List&lt;String&gt; should be used instead of String[] when creating an instance of the property.</li>
     * </ul>
     * <p>
     * Note: use {@link #isTypeNotNull()} instead to check if the property value can be null.
     */
    public boolean isNonPrimitiveFlag() {
        return isNonPrimitiveFlag;
    }

    /**
     * For a vector property, if it is a {@link java.util.List} or an array.
     * <p>
     * Currently only supported for {@link PropertyType#StringArray}.
     */
    public boolean isList() {
        return isList;
    }

    /**
     * If {@link PropertyFlags#UNSIGNED} should be set on this property.
     */
    public boolean isUnsigned() {
        return isUnsigned;
    }

    /**
     * If {@link PropertyFlags#ID_SELF_ASSIGNABLE} should be set on this property.
     */
    public boolean isIdAssignable() {
        return idAssignable;
    }

    /**
     * Returns the type including type parameters, e.g. {@code List<String>}.
     */
    public String getJavaType() {
        return javaType;
    }

    /**
     * If the Java type is generic, returns the raw type. E.g. {@code List} instead of {@code List<String>}.
     * Otherwise, returns the same as {@link #getJavaType()}.
     */
    public String getJavaRawType() {
        return javaRawType != null ? javaRawType : javaType;
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

    /**
     * If this property does not actually exist in the entity class, but only in the model
     * and {@link PropertyFlags#VIRTUAL} should be set.
     */
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

    @Nullable
    public HnswParams getHnswParams() {
        return hnswParams;
    }

    public boolean hasHnswParams() {
        return getHnswParams() != null;
    }

    /**
     * @return see {@link HnswParams#getExpression()}.
     */
    public String getHnswParamsExpression() {
        final HnswParams hnswParams = getHnswParams();
        return (hnswParams != null) ? hnswParams.getExpression() : "";
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
        // TODO: is it correct to use not-null type if using custom type?
        // Note: in Property-Type mapping String array is stored as the not null type, String list as the nullable type.
        if (isTypeNotNull()
                || customType != null
                || (propertyType == PropertyType.StringArray && !isList())) {
            javaType = schema.mapToJavaTypeNotNull(propertyType);
        } else {
            javaType = schema.mapToJavaTypeNullable(propertyType);
        }
        // Extract raw type if type is generic (e.g. extract List from List<String>)
        int typeArgumentsOpen = javaType.indexOf("<");
        if (typeArgumentsOpen != -1) {
            javaRawType = javaType.substring(0, typeArgumentsOpen);
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
     *
     * @see #getPropertyFlagsForModelFile()
     * @see #getPropertyFlagsForGeneratedCode()
     */
    public void computePropertyFlags() {
        int flagsModelFile = 0;
        Set<String> flagsGeneratedCode = new LinkedHashSet<>(); // keep in insert-order

        if (isPrimaryKey()) {
            flagsModelFile |= PropertyFlags.ID;
            flagsGeneratedCode.add("PropertyFlags.ID");
        }
        if (isIdAssignable()) {
            flagsModelFile |= PropertyFlags.ID_SELF_ASSIGNABLE;
            flagsGeneratedCode.add("PropertyFlags.ID_SELF_ASSIGNABLE");
        }
        if (isIdCompanion()) {
            flagsModelFile |= PropertyFlags.ID_COMPANION;
            flagsGeneratedCode.add("PropertyFlags.ID_COMPANION");
        }
        // Note: Primary key/ID properties must always be not null. Do not explicitly add this flag for them.
        if (isNotNullFlag() && !isPrimaryKey()) {
            flagsModelFile |= PropertyFlags.NOT_NULL;
            flagsGeneratedCode.add("PropertyFlags.NOT_NULL");
        }
        if (isNonPrimitiveFlag()) {
            // Note: not exported to model file, is specific to Java.
            flagsGeneratedCode.add("PropertyFlags.NON_PRIMITIVE_TYPE");
        }
        if (isVirtual()) {
            // Note: not exported to model file, is specific to Java.
            flagsGeneratedCode.add("PropertyFlags.VIRTUAL");
        }
        if (isUnsigned()) {
            flagsModelFile |= PropertyFlags.UNSIGNED;
            flagsGeneratedCode.add("PropertyFlags.UNSIGNED");
        }

        if (getPropertyType() == PropertyType.RelationId) {
            flagsModelFile |= PropertyFlags.INDEXED;
            flagsGeneratedCode.add("PropertyFlags.INDEXED");
            flagsModelFile |= PropertyFlags.INDEX_PARTIAL_SKIP_ZERO;
            flagsGeneratedCode.add("PropertyFlags.INDEX_PARTIAL_SKIP_ZERO");
        } else if (getIndex() != null) {
            flagsModelFile |= getIndex().getIndexFlags();
            flagsGeneratedCode.addAll(getIndex().getIndexFlagsAsNames());
        }

        this.propertyFlagsModelFile = flagsModelFile;
        this.propertyFlagsGeneratedCode = flagsGeneratedCode;
    }

    /**
     * Returns combined {@link io.objectbox.model.PropertyFlags} value of only those flags
     * that should be stored in the model file. Returns null if there would be no flags.
     * <p>
     * These may be different from {@link #getPropertyFlagsForGeneratedCode()}, see {@link #computePropertyFlags()}.
     */
    @Nullable
    public Integer getPropertyFlagsForModelFile() {
        if (propertyFlagsModelFile == null) {
            computePropertyFlags();
        }
        return propertyFlagsModelFile != 0 ? propertyFlagsModelFile : null;
    }

    /**
     * Returns names of {@link io.objectbox.model.PropertyFlags} to be used in generated model builder code.
     * <p>
     * These may be different from {@link #getPropertyFlagsForModelFile()}, see {@link #computePropertyFlags()}.
     */
    public Set<String> getPropertyFlagsForGeneratedCode() {
        if (propertyFlagsGeneratedCode == null) {
            computePropertyFlags();
        }
        return propertyFlagsGeneratedCode;
    }

    @Override
    public String toString() {
        return "Property " + propertyName + " of " + entity.getClassName();
    }

}
