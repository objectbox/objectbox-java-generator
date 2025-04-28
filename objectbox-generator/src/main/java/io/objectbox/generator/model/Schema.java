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

import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import io.objectbox.generator.IdUid;
import io.objectbox.generator.TextUtil;

/**
 * The "root" model class to which you can add entities to.
 */
public class Schema {
    public static final String DEFAULT_NAME = "default";

    private final int version;
    private final String defaultJavaPackage;
    private String defaultJavaPackageDao;
    private final List<Entity> entities;
    private Map<PropertyType, Mapping> propertyTypeMapping;
    private final String name;
    private final String prefix;
    private IdUid lastEntityId;
    private IdUid lastIndexId;
    private IdUid lastRelationId;
    private boolean isFinished;

    public Schema(String name, int version, String defaultJavaPackage) {
        this.name = name;
        this.prefix = name.equals(DEFAULT_NAME) ? "" : TextUtil.capFirst(name);
        this.version = version;
        this.defaultJavaPackage = defaultJavaPackage;
        this.entities = new ArrayList<>();
        initTypeMappings();
    }

    public Schema(int version, String defaultJavaPackage) {
        this(DEFAULT_NAME, version, defaultJavaPackage);
    }

    private static class Mapping {
        final short dbTypeId;
        final String dbType;
        final String javaTypeNullable;
        final String javaTypeNotNull;

        Mapping(short dbTypeId, String dbType, String javaTypeNullable, String javaTypeNotNull) {
            this.dbType = dbType;
            this.dbTypeId = dbTypeId;
            this.javaTypeNotNull = javaTypeNotNull;
            this.javaTypeNullable = javaTypeNullable;
        }
    }

    private void initTypeMappings() {
        propertyTypeMapping = new EnumMap<>(PropertyType.class);
        // Ordered like in io.objectbox.model.PropertyType
        propertyTypeMapping.put(PropertyType.Boolean, new Mapping(
                io.objectbox.model.PropertyType.Bool, "Bool", "Boolean", "boolean"));
        propertyTypeMapping.put(PropertyType.Byte, new Mapping(
                io.objectbox.model.PropertyType.Byte, "Byte", "Byte", "byte"));
        propertyTypeMapping.put(PropertyType.Short, new Mapping(
                io.objectbox.model.PropertyType.Short, "Short", "Short", "short"));
        propertyTypeMapping.put(PropertyType.Char, new Mapping(
                io.objectbox.model.PropertyType.Char, "Char", "Character", "char"));
        propertyTypeMapping.put(PropertyType.Int, new Mapping(
                io.objectbox.model.PropertyType.Int, "Int", "Integer", "int"));
        propertyTypeMapping.put(PropertyType.Long, new Mapping(
                io.objectbox.model.PropertyType.Long, "Long", "Long", "long"));
        propertyTypeMapping.put(PropertyType.Float, new Mapping(
                io.objectbox.model.PropertyType.Float, "Float", "Float", "float"));
        propertyTypeMapping.put(PropertyType.Double, new Mapping(
                io.objectbox.model.PropertyType.Double, "Double", "Double", "double"));
        propertyTypeMapping.put(PropertyType.String, new Mapping(
                io.objectbox.model.PropertyType.String, "String", "String", "String"));
        propertyTypeMapping.put(PropertyType.Date, new Mapping(
                io.objectbox.model.PropertyType.Date, "Date", "java.util.Date", "java.util.Date"));
        propertyTypeMapping.put(PropertyType.RelationId, new Mapping(
                io.objectbox.model.PropertyType.Relation, "Relation", "Long", "long"));
        propertyTypeMapping.put(PropertyType.DateNano, new Mapping(
                io.objectbox.model.PropertyType.DateNano, "DateNano", "Long", "long"));
        propertyTypeMapping.put(PropertyType.Flex, new Mapping(
                io.objectbox.model.PropertyType.Flex, "Flex", "byte[]", "byte[]"));
        propertyTypeMapping.put(PropertyType.BooleanArray, new Mapping(
                io.objectbox.model.PropertyType.BoolVector, "BoolVector", "boolean[]", "boolean[]"));
        propertyTypeMapping.put(PropertyType.ByteArray, new Mapping(
                io.objectbox.model.PropertyType.ByteVector, "ByteVector", "byte[]", "byte[]"));
        propertyTypeMapping.put(PropertyType.ShortArray, new Mapping(
                io.objectbox.model.PropertyType.ShortVector, "ShortVector", "short[]", "short[]"));
        propertyTypeMapping.put(PropertyType.CharArray, new Mapping(
                io.objectbox.model.PropertyType.CharVector, "CharVector", "char[]", "char[]"));
        propertyTypeMapping.put(PropertyType.IntArray, new Mapping(
                io.objectbox.model.PropertyType.IntVector, "IntVector", "int[]", "int[]"));
        propertyTypeMapping.put(PropertyType.LongArray, new Mapping(
                io.objectbox.model.PropertyType.LongVector, "LongVector", "long[]", "long[]"));
        propertyTypeMapping.put(PropertyType.FloatArray, new Mapping(
                io.objectbox.model.PropertyType.FloatVector, "FloatVector", "float[]", "float[]"));
        propertyTypeMapping.put(PropertyType.DoubleArray, new Mapping(
                io.objectbox.model.PropertyType.DoubleVector, "DoubleVector", "double[]", "double[]"));
        propertyTypeMapping.put(PropertyType.StringArray, new Mapping(
                io.objectbox.model.PropertyType.StringVector, "StringVector", "java.util.List<String>", "String[]"));
    }

    /**
     * Adds a new entity to the schema.
     */
    public Entity addEntity(String className) {
        Entity entity = new Entity(this, className);
        entities.add(entity);
        return entity;
    }

    public String mapToDbType(PropertyType propertyType) {
        return mapType(propertyTypeMapping, propertyType).dbType;
    }

    public short mapToDbTypeId(PropertyType propertyType) {
        return mapType(propertyTypeMapping, propertyType).dbTypeId;
    }

    public String mapToJavaTypeNullable(PropertyType propertyType) {
        return mapType(propertyTypeMapping, propertyType).javaTypeNullable;
    }

    public String mapToJavaTypeNotNull(PropertyType propertyType) {
        return mapType(propertyTypeMapping, propertyType).javaTypeNotNull;
    }

    private <T> T mapType(Map<PropertyType, T> map, PropertyType propertyType) {
        T dbType = map.get(propertyType);
        if (dbType == null) {
            throw new ModelRuntimeException("No mapping for " + propertyType);
        }
        return dbType;
    }

    public int getVersion() {
        return version;
    }

    public String getDefaultJavaPackage() {
        return defaultJavaPackage;
    }

    public String getDefaultJavaPackageDao() {
        return defaultJavaPackageDao;
    }

    public List<Entity> getEntities() {
        return entities;
    }

    public String getName() {
        return name;
    }

    public String getPrefix() {
        return prefix;
    }

    public IdUid getLastEntityId() {
        return lastEntityId;
    }

    public void setLastEntityId(IdUid lastEntityId) {
        this.lastEntityId = lastEntityId;
    }

    public IdUid getLastIndexId() {
        return lastIndexId;
    }

    public void setLastIndexId(IdUid lastIndexId) {
        this.lastIndexId = lastIndexId;
    }

    public IdUid getLastRelationId() {
        return lastRelationId;
    }

    public void setLastRelationId(IdUid lastRelationId) {
        this.lastRelationId = lastRelationId;
    }

    public boolean isFinished() {
        return isFinished;
    }

    /**
     * Sets DAO names for ObjectBox (Cursor), runs 2nd and 3rd pass on schema. Afterwards {@link #isFinished()}.
     */
    public void finish() throws ModelException {
        List<Entity> entities = getEntities();
        for (Entity entity : entities) {
            if (entity.getClassNameDao() == null) {
                entity.setClassNameDao(entity.getClassName() + "Cursor");
            }
        }

        init2ndPass();
        init3rdPass();

        // Order entities by name to ensure generated code is stable if no entities change
        getEntities().sort(Comparator.comparing(Entity::getDbName));

        isFinished = true;
    }

    private void init2ndPass() {
        if (defaultJavaPackageDao == null) {
            defaultJavaPackageDao = defaultJavaPackage;
        }
        for (Entity entity : entities) {
            entity.init2ndPass();
        }
    }

    private void init3rdPass() throws ModelException {
        for (Entity entity : entities) {
            entity.init3rdPass();
        }
    }

}
