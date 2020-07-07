<#--

Copyright (C) 2017-2018 ObjectBox Ltd.

This file is part of ObjectBox Build Tools.

ObjectBox Build Tools is free software: you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.
ObjectBox Build Tools is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with ObjectBox Build Tools.  If not, see <http://www.gnu.org/licenses/>.

-->
<#-- @ftlvariable name="entity" type="io.objectbox.generator.model.Entity" -->
<#-- @ftlvariable name="schema" type="io.objectbox.generator.model.Schema" -->
<#-- @ftlvariable name="imports" type="java.util.Set<String>" -->
<#-- @ftlvariable name="propertyCollector" type="java.lang.String" -->

<#assign toBindType = {"Boolean":"Long", "Byte":"Long", "Short":"Long", "Int":"Long", "Long":"Long", "Float":"Double", "Double":"Double", "String":"String", "ByteArray":"Blob", "Date": "Long" } />
<#assign toCursorType = {"Boolean":"Short", "Byte":"Short", "Short":"Short", "Int":"Int", "Long":"Long", "Float":"Float", "Double":"Double", "String":"String", "ByteArray":"Blob", "Date": "Long"  } />
<#if entity.javaPackageDao?length != 0>package ${entity.javaPackageDao};</#if>

<#list imports as import>
import ${import};
</#list>

// THIS CODE IS GENERATED BY ObjectBox, DO NOT EDIT.

/**
 * ObjectBox generated Cursor implementation for "${entity.dbName}".
 * Note that this is a low-level class: usually you should stick to the Box class.
 */
public final class ${entity.classNameDao} extends Cursor<${entity.className}> {
    @Internal
    static final class Factory implements CursorFactory<${entity.className}> {
        @Override
        public Cursor<${entity.className}> createCursor(io.objectbox.Transaction tx, long cursorHandle, BoxStore boxStoreForEntities) {
            return new ${entity.classNameDao}(tx, cursorHandle, boxStoreForEntities);
        }
    }

    private static final ${entity.className}_.${entity.className}IdGetter ID_GETTER = ${entity.className}_.__ID_GETTER;

<#list entity.properties as property><#if property.customType?has_content><#--
-->    private final ${property.converterClassName} ${property.propertyName}Converter = new ${property.converterClassName}();
</#if></#list>

<#-- Property IDs get verified in Cursor base class -->
<#list entity.properties as property>
    <#if !property.isPrimaryKey()>
    private final static int __ID_${property.propertyName} = ${entity.className}_.${property.propertyName}.id;
    </#if>
</#list>

    public ${entity.classNameDao}(io.objectbox.Transaction tx, long cursor, BoxStore boxStore) {
        super(tx, cursor, ${entity.className}_.__INSTANCE, boxStore);
    }

    @Override
    public final long getId(${entity.className} entity) {
        return ID_GETTER.getId(entity);
    }

    /**
     * Puts an object into its box.
<#if entity.protobuf>
     *
     * Note: Protocol buffer objects are immutable, so the ID cannot be updated (ID == 0, "insert").
</#if>
     *
     * @return The ID of the object within its box.
     */
    @Override
    public final long put(${entity.className} entity) {
<#list entity.toOneRelations as toOne>
        ToOne<${toOne.targetEntity.className}> ${toOne.nameToOne} = entity.${toOne.toOneValueExpression};
        if(${toOne.nameToOne} != null && ${toOne.nameToOne}.internalRequiresPutTarget()) {
            Cursor<${toOne.targetEntity.className}> targetCursor = getRelationTargetCursor(${toOne.targetEntity.className}.class);
            try {
                ${toOne.nameToOne}.internalPutTarget(targetCursor);
            } finally {
                targetCursor.close();
            }
        }
</#list>
<#--
Do ToOnes before because we need the target's ID before the put.
Do ToMany after because the targets entities need our ID.
-->
${propertyCollector}
<#if entity.hasRelations() && !entity.protobuf>
    <#if entity.hasBoxStoreField>
        entity.__boxStore = boxStoreForEntities;
    <#else>
        attachEntity(entity);
    </#if>
</#if>
<#list entity.toManyRelations as toMany>
        checkApplyToManyToDb(entity.${toMany.valueExpression}, ${toMany.targetEntity.className}.class);
</#list>
        return __assignedId;
    }

<#if entity.hasRelations() && !entity.hasBoxStoreField>
    private void attachEntity(${entity.className} entity) {
        // Transformer will create __boxStore field in entity and init it here:
        // entity.__boxStore = boxStoreForEntities;
    }

</#if>
}
