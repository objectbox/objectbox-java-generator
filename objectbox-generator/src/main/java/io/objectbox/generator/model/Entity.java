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
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import io.objectbox.generator.IdUid;
import io.objectbox.generator.TextUtil;
import io.objectbox.model.EntityFlags;
import io.objectbox.model.Model;

/**
 * Model class for an entity: a Java data object mapped to a data base representation. A new entity is added to a {@link
 * Schema}
 * by the method {@link Schema#addEntity(String)} (there is no public constructor for {@link Entity} itself). <br>
 * <br> Use the various addXXX methods to add entity properties, indexes, and relations to other entities (addToOne,
 * addToMany).<br> <br> There are further configuration possibilities: <ul> <li>{@link
 * Entity#implementsInterface(String...)} and {@link #implementsSerializable()} to specify interfaces the entity will
 * implement</li> <li>{@link #setSuperclass(String)} to specify a class of which the entity will extend from</li>
 * <li>Various setXXX methods</li> </ul>
 */
@SuppressWarnings("unused")
public class Entity implements HasParsedElement {
    private final Schema schema;
    private final String className;
    private Integer modelId;
    private Long modelUid;
    private IdUid lastPropertyId;
    private final List<Property> properties;
    private List<Property> propertiesColumns;
    private final List<Property> propertiesPk;
    private final List<Property> propertiesNonPk;

    /**
     * For fail fast checks which can show the stacktrace of the add operation
     * (there's another check after the 3rd init phase).
     */
    private final Map<String, Object> names;
    private final List<Index> indexes;
    private final List<Index> multiIndexes;
    private final List<ToOne> toOneRelations;
    private final List<ToManyBase> toManyRelations;
    private final List<ToManyBase> incomingToManyRelations;
    private final Collection<String> additionalImportsEntity;
    private final Collection<String> additionalImportsDao;
    private final List<String> interfacesToImplement;
    private final List<ContentProvider> contentProviders;

    private String dbName;
    private boolean nonDefaultDbName;
    private String classNameDao;
    private String classNameTest;
    private String javaPackage;
    private String javaPackageDao;
    private String javaPackageTest;
    private Property pkProperty;
    private String pkType;
    private String superclass;
    private String javaDoc;
    private String codeBeforeClass;

    private boolean protobuf;
    private boolean constructors;
    private boolean skipGeneration;
    private boolean skipGenerationTest;
    private boolean skipCreationInDb;
    private Boolean active;
    private Boolean hasKeepSections;
    private boolean hasBoxStoreField;
    private Object parsedElement;

    private Integer entityFlags;
    private Set<String> entityFlagsNames;

    Entity(Schema schema, String className) {
        this.schema = schema;
        this.className = className;
        properties = new ArrayList<>();
        propertiesPk = new ArrayList<>();
        propertiesNonPk = new ArrayList<>();
        names = new HashMap<>();
        indexes = new ArrayList<>();
        multiIndexes = new ArrayList<>();
        toOneRelations = new ArrayList<>();
        toManyRelations = new ArrayList<>();
        incomingToManyRelations = new ArrayList<>();
        additionalImportsEntity = new TreeSet<>();
        additionalImportsDao = new TreeSet<>();
        interfacesToImplement = new ArrayList<>();
        contentProviders = new ArrayList<>();
        constructors = true;
    }

    public Entity setModelId(Integer modelId) {
        this.modelId = modelId;
        return this;
    }

    public Integer getModelId() {
        return modelId;
    }

    public Entity setModelUid(Long modelUid) {
        this.modelUid = modelUid;
        return this;
    }

    public Long getModelUid() {
        return modelUid;
    }

    public IdUid getLastPropertyId() {
        return lastPropertyId;
    }

    public Entity setLastPropertyId(IdUid lastPropertyId) {
        this.lastPropertyId = lastPropertyId;
        return this;
    }

    public Property.PropertyBuilder addBooleanProperty(String propertyName) throws ModelException {
        return addProperty(PropertyType.Boolean, propertyName);
    }

    public Property.PropertyBuilder addByteProperty(String propertyName) throws ModelException {
        return addProperty(PropertyType.Byte, propertyName);
    }

    public Property.PropertyBuilder addShortProperty(String propertyName) throws ModelException {
        return addProperty(PropertyType.Short, propertyName);
    }

    public Property.PropertyBuilder addCharProperty(String propertyName) throws ModelException {
        return addProperty(PropertyType.Char, propertyName);
    }

    public Property.PropertyBuilder addIntProperty(String propertyName) throws ModelException {
        return addProperty(PropertyType.Int, propertyName);
    }

    public Property.PropertyBuilder addLongProperty(String propertyName) throws ModelException {
        return addProperty(PropertyType.Long, propertyName);
    }

    public Property.PropertyBuilder addFloatProperty(String propertyName) throws ModelException {
        return addProperty(PropertyType.Float, propertyName);
    }

    public Property.PropertyBuilder addDoubleProperty(String propertyName) throws ModelException {
        return addProperty(PropertyType.Double, propertyName);
    }

    public Property.PropertyBuilder addByteArrayProperty(String propertyName) throws ModelException {
        return addProperty(PropertyType.ByteArray, propertyName);
    }

    public Property.PropertyBuilder addStringProperty(String propertyName) throws ModelException {
        return addProperty(PropertyType.String, propertyName);
    }

    public Property.PropertyBuilder addDateProperty(String propertyName) throws ModelException {
        return addProperty(PropertyType.Date, propertyName);
    }

    public Property.PropertyBuilder addProperty(PropertyType propertyType, String propertyName) throws ModelException {
        Property.PropertyBuilder builder = new Property.PropertyBuilder(schema, this, propertyType, propertyName);
        Property property = builder.getProperty();
        trackUniqueName(names, propertyName, property);
        properties.add(property);
        return builder;
    }

    /** Adds a standard id property. */
    public Property.PropertyBuilder addIdProperty() throws ModelException {
        return addProperty(PropertyType.Long, "id").primaryKey();
    }

    /** Adds a to-many relationship; the target entity is joined to the PK property of this entity (typically the ID). */
    public ToMany addToMany(Entity target, Property targetProperty) throws ModelException {
        Property[] targetProperties = {targetProperty};
        return addToMany(null, target, targetProperties);
    }

    /**
     * Convenience method for {@link Entity#addToMany(Entity, Property)} with a subsequent call to {@link
     * ToMany#setName(String)}.
     */
    public ToMany addToMany(Entity target, Property targetProperty, String name) throws ModelException {
        ToMany toMany = addToMany(target, targetProperty);
        toMany.setName(name);
        trackUniqueName(names, name, toMany);
        return toMany;
    }

    /**
     * Adds a to-many relation linking back from a stand-alone to-many relation ({@link ToManyStandalone}).
     */
    public ToManyToMany addToMany(Entity target, String linkedToManyName, String name) throws ModelException {
        ToManyToMany toMany = new ToManyToMany(schema, this, target, linkedToManyName);
        toMany.setName(name);
        trackUniqueName(names, name, toMany);
        addToMany(toMany);
        return toMany;
    }

    /**
     * Adds a stand-alone to-many relation ({@link ToManyStandalone}).
     */
    public ToManyStandalone addToManyStandalone(Entity target, String name) throws ModelException {
        ToManyStandalone toMany = new ToManyStandalone(schema, this, target);
        toMany.setName(name);
        trackUniqueName(names, name, toMany);
        addToMany(toMany);
        return toMany;
    }

    /**
     * Add a to-many relationship; the target entity is joined using the given target property (of the target entity)
     * and given source property (of this entity).
     */
    public ToMany addToMany(Property sourceProperty, Entity target, Property targetProperty) throws ModelException {
        Property[] sourceProperties = {sourceProperty};
        Property[] targetProperties = {targetProperty};
        return addToMany(sourceProperties, target, targetProperties);
    }

    public ToMany addToMany(Property[] sourceProperties, Entity target, Property[] targetProperties) throws ModelException {
        ToMany toMany = new ToMany(schema, this, sourceProperties, target, targetProperties);
        addToMany(toMany);
        return toMany;
    }

    public void addToMany(ToManyBase toMany) throws ModelException {
        if (protobuf) {
            throw new ModelException("Protobuf entities do not support relations, currently");
        }

        toManyRelations.add(toMany);
        toMany.targetEntity.incomingToManyRelations.add(toMany);
    }

    /**
     * Adds a to-one relationship to the given target entity using the given given foreign key property (which belongs
     * to this entity).
     */
    public ToOne addToOne(Entity target, Property targetIdProperty, String name, String nameToOne,
                          boolean toOneFieldAccessible) throws ModelException {
        if (protobuf) {
            throw new ModelException("Protobuf entities do not support relations, currently");
        }

        targetIdProperty.convertToRelationId(target);
        ToOne toOne = new ToOne(schema, this, target, targetIdProperty, true);
        toOne.setName(name);
        toOne.setNameToOne(nameToOne);
        toOne.setToOneFieldAccessible(toOneFieldAccessible);
        toOneRelations.add(toOne);
        trackUniqueName(names, name, toOne);
        if (nameToOne != null && !nameToOne.equals(name)) {
            trackUniqueName(names, nameToOne, toOne);
        }
        return toOne;
    }

    protected void addIncomingToMany(ToMany toMany) {
        incomingToManyRelations.add(toMany);
    }

    public ContentProvider addContentProvider() {
        List<Entity> entities = new ArrayList<>();
        entities.add(this);
        ContentProvider contentProvider = new ContentProvider(schema, entities);
        contentProviders.add(contentProvider);
        return contentProvider;
    }

    /** Adds a new index to the entity. */
    public Entity addIndex(Index index) {
        indexes.add(index);
        return this;
    }

    public Entity addImport(String additionalImport) {
        additionalImportsEntity.add(additionalImport);
        return this;
    }

    /** The entity is represented by a protocol buffers object. Requires some special actions like using builders. */
    Entity useProtobuf() {
        protobuf = true;
        return this;
    }

    public boolean isProtobuf() {
        return protobuf;
    }

    public Schema getSchema() {
        return schema;
    }

    public String getDbName() {
        return dbName;
    }

    public void setDbName(String dbName) {
        this.dbName = dbName;
        this.nonDefaultDbName = dbName != null;
    }

    public String getClassName() {
        return className;
    }

    public List<Property> getProperties() {
        return properties;
    }

    public Property findPropertyByName(String name) {
        for (Property property : properties) {
            if (name.equals(property.getPropertyName())) {
                return property;
            }
        }
        return null;
    }

    public Property findPropertyByNameOrThrow(String name) throws ModelException {
        Property property = findPropertyByName(name);
        if (property == null) {
            throw new ModelException("Could not find property " + name + " in " + name);
        }
        return property;
    }

    public List<Property> getPropertiesColumns() {
        return propertiesColumns;
    }

    public String getJavaPackage() {
        return javaPackage;
    }

    public void setJavaPackage(String javaPackage) {
        this.javaPackage = javaPackage;
    }

    public String getJavaPackageDao() {
        return javaPackageDao;
    }

    public void setJavaPackageDao(String javaPackageDao) {
        this.javaPackageDao = javaPackageDao;
    }

    public String getClassNameDao() {
        return classNameDao;
    }

    public void setClassNameDao(String classNameDao) {
        this.classNameDao = classNameDao;
    }

    public String getClassNameTest() {
        return classNameTest;
    }

    public void setClassNameTest(String classNameTest) {
        this.classNameTest = classNameTest;
    }

    public String getJavaPackageTest() {
        return javaPackageTest;
    }

    public void setJavaPackageTest(String javaPackageTest) {
        this.javaPackageTest = javaPackageTest;
    }

    /** Internal property used by templates, don't use during entity definition. */
    public List<Property> getPropertiesPk() {
        return propertiesPk;
    }

    /** Internal property used by templates, don't use during entity definition. */
    public List<Property> getPropertiesNonPk() {
        return propertiesNonPk;
    }

    /** Internal property used by templates, don't use during entity definition. */
    public Property getPkProperty() {
        return pkProperty;
    }

    public List<Index> getIndexes() {
        return indexes;
    }

    /** Internal property used by templates, don't use during entity definition. */
    public String getPkType() {
        return pkType;
    }

    public boolean isConstructors() {
        return constructors;
    }

    /** Flag to define if constructors should be generated. */
    public void setConstructors(boolean constructors) {
        this.constructors = constructors;
    }

    public boolean isSkipGeneration() {
        return skipGeneration;
    }

    /**
     * Flag if the entity's code generation should be skipped. E.g. if you need to change the class after initial
     * generation.
     */
    public void setSkipGeneration(boolean skipGeneration) {
        this.skipGeneration = skipGeneration;
    }

    /** For secondary entities that depend on the schema of a primary entity. */
    public void setSkipCreationInDb(boolean skipCreationInDb) {
        this.skipCreationInDb = skipCreationInDb;
    }

    public boolean isSkipCreationInDb() {
        return skipCreationInDb;
    }

    public boolean isSkipGenerationTest() {
        return skipGenerationTest;
    }

    public void setSkipGenerationTest(boolean skipGenerationTest) {
        this.skipGenerationTest = skipGenerationTest;
    }

    public boolean hasRelations() {
        return !toOneRelations.isEmpty() || !toManyRelations.isEmpty();
    }

    public List<ToOne> getToOneRelations() {
        return toOneRelations;
    }

    public List<ToManyBase> getToManyRelations() {
        return toManyRelations;
    }

    public List<ToManyBase> getIncomingToManyRelations() {
        return incomingToManyRelations;
    }

    /**
     * Entities with relations are active, but this method allows to make the entities active even if it does not have
     * relations.
     */
    public void setActive(Boolean active) {
        this.active = active;
    }

    public Boolean getActive() {
        return active;
    }

    public Boolean getHasKeepSections() {
        return hasKeepSections;
    }

    public Collection<String> getAdditionalImportsEntity() {
        return additionalImportsEntity;
    }

    public Collection<String> getAdditionalImportsDao() {
        return additionalImportsDao;
    }

    public void setHasKeepSections(Boolean hasKeepSections) {
        this.hasKeepSections = hasKeepSections;
    }

    public List<String> getInterfacesToImplement() {
        return interfacesToImplement;
    }

    public List<ContentProvider> getContentProviders() {
        return contentProviders;
    }

    public void implementsInterface(String... interfaces) throws ModelException {
        for (String interfaceToImplement : interfaces) {
            if (interfacesToImplement.contains(interfaceToImplement)) {
                throw new ModelException("Interface defined more than once: " + interfaceToImplement);
            }
            interfacesToImplement.add(interfaceToImplement);
        }
    }

    public void implementsSerializable() {
        interfacesToImplement.add("java.io.Serializable");
    }

    public String getSuperclass() {
        return superclass;
    }

    public void setSuperclass(String classToExtend) {
        this.superclass = classToExtend;
    }

    public String getJavaDoc() {
        return javaDoc;
    }

    public void setJavaDoc(String javaDoc) {
        this.javaDoc = TextUtil.checkConvertToJavaDoc(javaDoc, "");
    }

    public String getCodeBeforeClass() {
        return codeBeforeClass;
    }

    public void setCodeBeforeClass(String codeBeforeClass) {
        this.codeBeforeClass = codeBeforeClass;
    }

    public boolean getHasBoxStoreField() {
        return hasBoxStoreField;
    }

    public void setHasBoxStoreField(boolean hasBoxStoreField) {
        this.hasBoxStoreField = hasBoxStoreField;
    }

    void init2ndPass() throws ModelException {
        init2ndPassNamesWithDefaults();

        for (int i = 0; i < properties.size(); i++) {
            Property property = properties.get(i);
            property.setOrdinal(i);
            property.init2ndPass();
            if (property.isPrimaryKey()) {
                propertiesPk.add(property);
            } else {
                propertiesNonPk.add(property);
            }
        }

        for (int i = 0; i < indexes.size(); i++) {
            final Index index = indexes.get(i);
            final int propertiesSize = index.getProperties().size();
            if (propertiesSize == 1) {
                final Property property = index.getProperties().get(0);
                property.setIndex(index);
            } else if (propertiesSize > 1) {
                multiIndexes.add(index);
            }
        }

        if (propertiesPk.size() == 1) {
            pkProperty = propertiesPk.get(0);
            pkType = schema.mapToJavaTypeNullable(pkProperty.getPropertyType());
        } else {
            pkType = "Void";
        }

        propertiesColumns = new ArrayList<>(properties);
        for (ToOne toOne : toOneRelations) {
            toOne.init2ndPass();
            Property targetIdProperty = toOne.getTargetIdProperty();
            if (!propertiesColumns.contains(targetIdProperty)) {
                propertiesColumns.add(targetIdProperty);
            }
        }

        for (ToManyBase toMany : toManyRelations) {
            toMany.init2ndPass();
            // Source Properties may not be virtual, so we do not need the following code:
            // for (Property sourceProperty : toMany.getSourceProperties()) {
            // if (!propertiesColumns.contains(sourceProperty)) {
            // propertiesColumns.add(sourceProperty);
            // }
            // }
        }

        if (active == null) {
            active = schema.isUseActiveEntitiesByDefault();
        }
        active |= !toOneRelations.isEmpty() || !toManyRelations.isEmpty();

        if (hasKeepSections == null) {
            hasKeepSections = schema.isHasKeepSectionsByDefault();
        }

        init2ndPassIndexNamesWithDefaults();

        for (ContentProvider contentProvider : contentProviders) {
            contentProvider.init2ndPass();
        }
    }

    protected void init2ndPassNamesWithDefaults() {
        if (dbName == null) {
            dbName = TextUtil.dbName(className);
            nonDefaultDbName = false;
        }

        if (classNameDao == null) {
            classNameDao = className + "Dao";
        }
        if (classNameTest == null) {
            classNameTest = className + "Test";
        }

        if (javaPackage == null) {
            javaPackage = schema.getDefaultJavaPackage();
        }

        if (javaPackageDao == null) {
            javaPackageDao = schema.getDefaultJavaPackageDao();
            if (javaPackageDao == null) {
                javaPackageDao = javaPackage;
            }
        }
        if (javaPackageTest == null) {
            javaPackageTest = schema.getDefaultJavaPackageTest();
            if (javaPackageTest == null) {
                javaPackageTest = javaPackage;
            }
        }
    }

    protected void init2ndPassIndexNamesWithDefaults() {
        for (int i = 0; i < indexes.size(); i++) {
            Index index = indexes.get(i);
            if (index.getName() == null) {
                String indexName = "IDX_" + getDbName();
                List<Property> properties = index.getProperties();
                for (int j = 0; j < properties.size(); j++) {
                    Property property = properties.get(j);
                    indexName += "_" + property.getDbName();
                    if ("DESC".equalsIgnoreCase(index.getPropertiesOrder().get(j))) {
                        indexName += "_DESC";
                    }
                }
                // TODO can this get too long? how to shorten reliably without depending on the order (i)
                index.setDefaultName(indexName);
            }
        }
    }

    void init3rdPass() throws ModelException {
        for (Property property : properties) {
            property.init3ndPass();
        }

        init3rdPassRelations();
        init3rdPassAdditionalImports();

        // Check again, because names may have changed during 2nd or 3rd pass
        verifyAllNamesUnique();
    }

    private void verifyAllNamesUnique() throws ModelException {
        Map<String, Object> names = new HashMap<>();
        for (Property property : properties) {
            trackUniqueName(names, property.getPropertyName(), property);
        }
        for (ToOne toOne : toOneRelations) {
            trackUniqueName(names, toOne.getName(), toOne);
            if (toOne.getNameToOne() != null && toOne.getNameToOne() != toOne.getName()) {
                trackUniqueName(names, toOne.getNameToOne(), toOne);
            }
        }
        for (ToManyBase toMany : toManyRelations) {
            trackUniqueName(names, toMany.getName(), toMany);
        }
    }

    private void trackUniqueName(Map<String, Object> names, String name, Object object) throws ModelException {
        Object oldValue = names.put(name.toLowerCase(), object);
        if (oldValue != null) {
            throw new ModelException("Duplicate name \"" + name + "\" in entity \"" + className +
                    "\": [" + object + "] vs. [" + oldValue + "]");
        }
    }

    private void init3rdPassRelations() throws ModelException {
        for (ToOne toOne : toOneRelations) {
            toOne.init3ndPass();
        }
        for (ToManyBase toMany : toManyRelations) {
            toMany.init3rdPass();
            if (toMany instanceof ToMany) {
                Entity targetEntity = toMany.getTargetEntity();
                for (Property targetProperty : ((ToMany) toMany).getTargetProperties()) {
                    if (!targetEntity.propertiesColumns.contains(targetProperty)) {
                        targetEntity.propertiesColumns.add(targetProperty);
                    }
                }
            }
        }
    }

    private void init3rdPassAdditionalImports() {
        if (active && !javaPackage.equals(javaPackageDao)) {
            additionalImportsEntity.add(javaPackageDao + "." + classNameDao);
        }

        for (ToOne toOne : toOneRelations) {
            Entity targetEntity = toOne.getTargetEntity();
            checkAdditionalImportsEntityTargetEntity(targetEntity);
            checkAdditionalImportsDaoTargetEntity(targetEntity);
        }

        for (ToManyBase toMany : toManyRelations) {
            Entity targetEntity = toMany.getTargetEntity();
            checkAdditionalImportsEntityTargetEntity(targetEntity);
            checkAdditionalImportsDaoTargetEntity(targetEntity);
        }

        for (Property property : properties) {
            String customType = property.getCustomType();
            if (customType != null) {
                String pack = TextUtil.getPackageFromFullyQualified(customType);
                if (pack != null && !pack.equals(javaPackage)) {
                    additionalImportsEntity.add(customType);
                }
                if (pack != null && !pack.equals(javaPackageDao)) {
                    additionalImportsDao.add(customType);
                }
            }

            String converter = property.getConverter();
            if (converter != null) {
                String pack = TextUtil.getPackageFromFullyQualified(converter);
                if (pack != null && !pack.equals(javaPackageDao)) {
                    additionalImportsDao.add(converter);
                }
            }

        }
    }

    private void checkAdditionalImportsEntityTargetEntity(Entity targetEntity) {
        if (!targetEntity.getJavaPackage().equals(javaPackage)) {
            additionalImportsEntity.add(targetEntity.getJavaPackage() + "." + targetEntity.getClassName());
        }
        if (!targetEntity.getJavaPackageDao().equals(javaPackage)) {
            additionalImportsEntity.add(targetEntity.getJavaPackageDao() + "." + targetEntity.getClassNameDao());
        }
    }

    private void checkAdditionalImportsDaoTargetEntity(Entity targetEntity) {
        if (!targetEntity.getJavaPackage().equals(javaPackageDao)) {
            additionalImportsDao.add(targetEntity.getJavaPackage() + "." + targetEntity.getClassName());
        }
    }

    public void validatePropertyExists(Property property) throws ModelException {
        if (!properties.contains(property)) {
            throw new ModelException("Property " + property + " does not exist in " + this);
        }
    }

    public List<Index> getMultiIndexes() {
        return multiIndexes;
    }

    public boolean isNonDefaultDbName() {
        return nonDefaultDbName;
    }

    public Object getParsedElement() {
        return parsedElement;
    }

    public void setParsedElement(Object parsedElement) {
        this.parsedElement = parsedElement;
    }

    /**
     * Based on this entities attributes computes required {@link EntityFlags}.
     * @see #getEntityFlags()
     * @see #getEntityFlagsNames()
     */
    public void computeEntityFlags() {
        int flags = 0;
        Set<String> flagsNames = new LinkedHashSet<>(); // keep in insert-order

        if (!isConstructors()) {
            flags |= EntityFlags.USE_NO_ARG_CONSTRUCTOR;
            flagsNames.add("io.objectbox.model.EntityFlags.USE_NO_ARG_CONSTRUCTOR");
        }

        this.entityFlags = flags;
        this.entityFlagsNames = flagsNames;
    }

    /**
     * Returns combined {@link io.objectbox.model.EntityFlags} value.
     */
    public int getEntityFlags() {
        if (entityFlags == null) {
            computeEntityFlags();
        }
        return entityFlags;
    }

    /**
     * Returns names of {@link io.objectbox.model.EntityFlags}.
     */
    public Set<String> getEntityFlagsNames() {
        if (entityFlagsNames == null) {
            computeEntityFlags();
        }
        return entityFlagsNames;
    }

    @Override
    public String toString() {
        return "Entity " + className + " (package: " + javaPackage + ")";
    }

}
