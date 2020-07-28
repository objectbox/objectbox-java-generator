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

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import io.objectbox.generator.IdUid;
import io.objectbox.generator.TextUtil;

/**
 * The "root" model class to which you can add entities to.
 */
@SuppressWarnings("unused")
public class Schema {
    public static final String DEFAULT_NAME = "default";

    private final int version;
    private final String defaultJavaPackage;
    private String defaultJavaPackageDao;
    private String defaultJavaPackageTest;
    private final List<Entity> entities;
    private Map<PropertyType, Mapping> propertyTypeMapping;
    private boolean hasKeepSectionsByDefault;
    private boolean useActiveEntitiesByDefault;
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

    public void enableKeepSectionsByDefault() {
        hasKeepSectionsByDefault = true;
    }

    public void enableActiveEntitiesByDefault() {
        useActiveEntitiesByDefault = true;
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
        propertyTypeMapping.put(PropertyType.Boolean, new Mapping(
                io.objectbox.model.PropertyType.Bool, "Bool", "Boolean", "boolean"));
        propertyTypeMapping.put(PropertyType.Byte, new Mapping(
                io.objectbox.model.PropertyType.Byte, "Byte", "Byte", "byte"));
        propertyTypeMapping.put(PropertyType.Char, new Mapping(
                io.objectbox.model.PropertyType.Char, "Char", "Character", "char"));
        propertyTypeMapping.put(PropertyType.Short, new Mapping(
                io.objectbox.model.PropertyType.Short, "Short", "Short", "short"));
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
        propertyTypeMapping.put(PropertyType.ByteArray, new Mapping(
                io.objectbox.model.PropertyType.ByteVector, "ByteVector", "byte[]", "byte[]"));
        propertyTypeMapping.put(PropertyType.Date, new Mapping(
                io.objectbox.model.PropertyType.Date, "Date", "java.util.Date", "java.util.Date"));
        propertyTypeMapping.put(PropertyType.RelationId, new Mapping(
                io.objectbox.model.PropertyType.Relation, "Relation", "Long", "long"));
    }

    /**
     * Adds a new entity to the schema. There can be multiple entities per table, but only one may be the primary entity
     * per table to create table scripts, etc.
     */
    public Entity addEntity(String className) {
        Entity entity = new Entity(this, className);
        entities.add(entity);
        return entity;
    }

    /**
     * Adds a new protocol buffers entity to the schema. There can be multiple entities per table, but only one may be
     * the primary entity per table to create table scripts, etc.
     */
    public Entity addProtobufEntity(String className) {
        Entity entity = addEntity(className);
        entity.useProtobuf();
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

    public void setDefaultJavaPackageDao(String defaultJavaPackageDao) {
        this.defaultJavaPackageDao = defaultJavaPackageDao;
    }

    public String getDefaultJavaPackageTest() {
        return defaultJavaPackageTest;
    }

    public void setDefaultJavaPackageTest(String defaultJavaPackageTest) {
        this.defaultJavaPackageTest = defaultJavaPackageTest;
    }

    public List<Entity> getEntities() {
        return entities;
    }

    public boolean isHasKeepSectionsByDefault() {
        return hasKeepSectionsByDefault;
    }

    public boolean isUseActiveEntitiesByDefault() {
        return useActiveEntitiesByDefault;
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

        isFinished = true;
    }

    void init2ndPass() {
        if (defaultJavaPackageDao == null) {
            defaultJavaPackageDao = defaultJavaPackage;
        }
        if (defaultJavaPackageTest == null) {
            defaultJavaPackageTest = defaultJavaPackageDao;
        }
        for (Entity entity : entities) {
            entity.init2ndPass();
        }
    }

    void init3rdPass() throws ModelException {
        for (Entity entity : entities) {
            entity.init3rdPass();
        }
    }

}
