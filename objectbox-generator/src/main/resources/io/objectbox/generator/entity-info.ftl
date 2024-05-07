<#--

ObjectBox Build Tools
Copyright (C) 2017-2024 ObjectBox Ltd.

This program is free software: you can redistribute it and/or modify
it under the terms of the GNU Affero General Public License as published
by the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU Affero General Public License for more details.

You should have received a copy of the GNU Affero General Public License
along with this program.  If not, see <https://www.gnu.org/licenses/>.

-->
<#-- @ftlvariable name="entity" type="io.objectbox.generator.model.Entity" -->
<#-- @ftlvariable name="schema" type="io.objectbox.generator.model.Schema" -->
<#-- @ftlvariable name="imports" type="java.util.Set<String>" -->

<#if entity.javaPackageDao?length != 0>
package ${entity.javaPackageDao};
</#if>

<#list imports as import>
import ${import};
</#list>

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
    property.modelId??>${property.modelId.id?c}<#else>0</#if>, ${property.javaRawType}.class, "${property.propertyName}"<#if property.isVirtual()>, true</#if><#if
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
<#if !entity.pkProperty.isTypeNotNull()>
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
    --> ${toOne.idRefPropertyName},<#--
    --> new ToOneGetter<${toOne.sourceEntity.className}, ${toOne.targetEntity.className}>() {
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
            new ToManyGetter<${toMany.sourceEntity.className}, ${toMany.targetEntity.className}>() {
                @Override
                public List<${toMany.targetEntity.className}> getToMany(${toMany.sourceEntity.className} entity) {
                    return entity.${toMany.valueExpression};
                }
            },
            <#-- Instead of checking if instance is ToManyByBacklink, use Freemarker to check for properties that only it has. -->
            <#if toMany.targetToOne??>${toMany.targetEntity.className}_.${toMany.targetToOne.idRefPropertyName},
            new ToOneGetter<${toMany.targetEntity.className}, ${toMany.sourceEntity.className}>() {
                @Override
                public ToOne<${toMany.sourceEntity.className}> getToOne(${toMany.targetEntity.className} entity) {
                    return entity.${toMany.targetToOne.toOneValueExpression};
                }
            }<#else><#if toMany.targetToMany??>new ToManyGetter<${toMany.targetEntity.className}, ${toMany.sourceEntity.className}>() {
                @Override
                public List<${toMany.sourceEntity.className}> getToMany(${toMany.targetEntity.className} entity) {
                    return entity.${toMany.targetToMany.valueExpression};
                }
            }, ${toMany.targetToMany.modelId.id}<#else> ${toMany.modelId.id}</#if></#if>);

    </#list>
</#if>
}
