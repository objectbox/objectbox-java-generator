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

<#if entity.javaPackageDao?length != 0>
package ${entity.javaPackageDao};

import ${entity.javaPackageDao}.${entity.classNameDao}.Factory;
</#if>
<#-- Note: Using fully qualified imports for ObjectBox classes which names are likely
to conflict with user-defined entity classes. -->
import io.objectbox.EntityInfo;
import io.objectbox.annotation.apihint.Internal;
import io.objectbox.internal.CursorFactory;
import io.objectbox.internal.IdGetter;
<#if entity.hasRelations()>
import io.objectbox.relation.RelationInfo;
import io.objectbox.relation.ToOne;
import io.objectbox.internal.ToOneGetter;
<#if entity.toManyRelations?has_content>
import io.objectbox.internal.ToManyGetter;

import java.util.List;
</#if>
<#list entity.toOneRelations as toOne>
<#if toOne.targetEntity.javaPackageDao != entity.javaPackageDao && toOne.targetEntity.javaPackageDao?length != 0>
import ${toOne.targetEntity.javaPackageDao}.${toOne.targetEntity.className}_;
</#if>
</#list>
<#list entity.toManyRelations as toMany>
<#if toMany.targetEntity.javaPackageDao != entity.javaPackageDao && toMany.targetEntity.javaPackageDao?length != 0>
import ${toMany.targetEntity.javaPackageDao}.${toMany.targetEntity.className}_;
</#if>
</#list>
</#if>

<#-- For custom types only here. TODO: do not import relation stuff -->
<#if entity.additionalImportsDao?has_content>
<#list entity.additionalImportsDao as additionalImport>
import ${additionalImport};
</#list>
</#if>

// THIS CODE IS GENERATED BY ObjectBox, DO NOT EDIT.

/**
 * Properties for entity "${entity.dbName}". Can be used for QueryBuilder and for referencing DB names.
 */
public final class ${entity.className}_ implements EntityInfo<${entity.className}> {

    // Leading underscores for static constants to avoid naming conflicts with property names

    public static final String __ENTITY_NAME = "${entity.className}";

    public static final int __ENTITY_ID = ${entity.modelId?c};

    public static final Class<${entity.className}> __ENTITY_CLASS = ${entity.className}.class;

    public static final String __DB_NAME = "${entity.dbName}";

    public static final CursorFactory<${entity.className}> __CURSOR_FACTORY = new <#if entity.javaPackageDao?length == 0>${entity.classNameDao}.</#if>Factory();

    @Internal
    static final ${entity.className}IdGetter __ID_GETTER = new ${entity.className}IdGetter();

    public final static ${entity.className}_ __INSTANCE = new ${entity.className}_();

<#list entity.propertiesColumns as property>
    public final static io.objectbox.Property<${entity.className}> ${property.propertyName} =
        new io.objectbox.Property<>(__INSTANCE, ${property_index}, <#if
    property.modelId??>${property.modelId.id?c}<#else>0</#if>, ${property.javaType}.class, "${property.propertyName}"<#if
    property.primaryKey || (property.dbName?? && property.dbName != property.propertyName) || property.converter??>, ${property.primaryKey?string}, "${property.dbName}"<#if
property.converter??>, ${property.converterClassName}.class, ${property.customTypeClassName}.class</#if></#if>);

</#list>
    @SuppressWarnings("unchecked")
    public final static io.objectbox.Property<${entity.className}>[] __ALL_PROPERTIES = new io.objectbox.Property[]{
<#list entity.propertiesColumns as property>
        ${property.propertyName}<#if property?has_next>,</#if>
</#list>
    };

    public final static io.objectbox.Property<${entity.className}> __ID_PROPERTY = ${entity.pkProperty.propertyName};

    @Override
    public String getEntityName() {
        return __ENTITY_NAME;
    }

    @Override
    public int getEntityId() {
        return __ENTITY_ID;
    }

    @Override
    public Class<${entity.className}> getEntityClass() {
        return __ENTITY_CLASS;
    }

    @Override
    public String getDbName() {
        return __DB_NAME;
    }

    @Override
    public io.objectbox.Property<${entity.className}>[] getAllProperties() {
        return __ALL_PROPERTIES;
    }

    @Override
    public io.objectbox.Property<${entity.className}> getIdProperty() {
        return __ID_PROPERTY;
    }

    @Override
    public IdGetter<${entity.className}> getIdGetter() {
        return __ID_GETTER;
    }

    @Override
    public CursorFactory<${entity.className}> getCursorFactory() {
        return __CURSOR_FACTORY;
    }

    @Internal
    static final class ${entity.className}IdGetter implements IdGetter<${entity.className}> {
        @Override
        public long getId(${entity.className} object) {
<#if entity.pkProperty.nonPrimitiveType>
            ${entity.pkProperty.javaType} id = object.${entity.pkProperty.valueExpression};
            return id != null? id : 0;
<#else>
            return object.${entity.pkProperty.valueExpression};
</#if>
        }
    }

<#--
^^^^ Up to here we did not reference any other entity-info classes.
     Thus, relations may reference all fields above to ensure correct initialization.
     Explanation: if a static field in class A needs a static field in class B at some point,
     it will switch to completely init class B leaving class A only partially initialized until init of B is complete.
     For entity infos, partial initialization of the above fields is guaranteed.
-->
<#if entity.hasRelations() >
    <#list entity.toOneRelations as toOne>
    /** To-one relation "${toOne.name}" to target entity "${toOne.targetEntity.className}". */
    public static final RelationInfo<${toOne.sourceEntity.className}, ${toOne.targetEntity.className}> ${toOne.name} =
            new RelationInfo<>(${toOne.sourceEntity.className}_.__INSTANCE,<#--
    --> ${toOne.targetEntity.className}_.__INSTANCE,<#--
    --> ${toOne.targetIdProperty.propertyName},<#--
    --> new ToOneGetter<${toOne.sourceEntity.className}>() {
                @Override
                public ToOne<${toOne.targetEntity.className}> getToOne(${toOne.sourceEntity.className} entity) {
                    return entity.${toOne.toOneValueExpression};
                }
            });

    </#list>
    <#list entity.toManyRelations as toMany>
    /** To-many relation "${toMany.name}" to target entity "${toMany.targetEntity.className}". */
    public static final RelationInfo<${toMany.sourceEntity.className}, ${toMany.targetEntity.className}> ${toMany.name} =<#--
     --> new RelationInfo<>(${toMany.sourceEntity.className}_.__INSTANCE,<#--
     --> ${toMany.targetEntity.className}_.__INSTANCE,
            new ToManyGetter<${toMany.sourceEntity.className}>() {
                @Override
                public List<${toMany.targetEntity.className}> getToMany(${toMany.sourceEntity.className} entity) {
                    return entity.${toMany.valueExpression};
                }
            },
            <#if toMany.targetProperties??>${toMany.targetEntity.className}_.${toMany.targetProperties[0].propertyName},
            new ToOneGetter<${toMany.targetEntity.className}>() {
                @Override
                public ToOne<${toMany.sourceEntity.className}> getToOne(${toMany.targetEntity.className} entity) {
                    return entity.${toMany.backlinkToOne.toOneValueExpression};
                }
            }<#else><#if toMany.backlinkToMany??>new ToManyGetter<${toMany.targetEntity.className}>() {
                @Override
                public List<${toMany.sourceEntity.className}> getToMany(${toMany.targetEntity.className} entity) {
                    return entity.${toMany.backlinkToMany.valueExpression};
                }
            }, ${toMany.backlinkToMany.modelId.id}<#else> ${toMany.modelId.id}</#if></#if>);

    </#list>
</#if>
}
