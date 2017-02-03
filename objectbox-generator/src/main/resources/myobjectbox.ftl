<#--

Copyright (C) 2017 Markus Junginger, greenrobot (http://greenrobot.org)

This file is part of ObjectBox Generator.

ObjectBox Generator is free software: you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.
ObjectBox Generator is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with ObjectBox Generator.  If not, see <http://www.gnu.org/licenses/>.

-->
<#-- @ftlvariable name="schema" type="io.objectbox.generator.model.Schema" -->
package ${schema.defaultJavaPackageDao};

import io.objectbox.BoxStore;
import io.objectbox.BoxStoreBuilder;
import io.objectbox.ModelBuilder;
import io.objectbox.ModelBuilder.EntityBuilder;
import io.objectbox.model.PropertyFlags;
import io.objectbox.model.PropertyType;

<#list schema.entities as entity>
<#if entity.javaPackage != schema.defaultJavaPackageDao>
import ${entity.javaPackage}.${entity.className};
</#if>
</#list>
<#list schema.entities as entity>
<#if entity.javaPackageDao != schema.defaultJavaPackageDao>
import ${entity.javaPackageDao}.${entity.classNameDao};
import ${entity.javaPackageDao}.${entity.className}_;
</#if>
</#list>

// THIS CODE IS GENERATED BY ObjectBox, DO NOT EDIT.
/**
 * Starting point for working with your ObjectBox. All boxes are set up for your objects here.
 * <p>
 * First steps (Android): get a builder using {@link #builder()}, call {@link BoxStoreBuilder#androidContext(Object)},
 * and {@link BoxStoreBuilder#build()} to get a {@link BoxStore} to work with.
 */
public class MyObjectBox {

    public static BoxStoreBuilder builder() {
        BoxStoreBuilder builder = new BoxStoreBuilder(getModel());
<#list schema.entities as entity>
        builder.entity("${entity.dbName}", ${entity.className}.class, ${entity.classNameDao}.class, new ${entity.className}_());
</#list>
        return builder;
    }

    private static byte[] getModel() {
        ModelBuilder modelBuilder = new ModelBuilder();
<#if schema.lastEntityId??>
        modelBuilder.lastEntityId(${schema.lastEntityId?c});
</#if>
<#if schema.lastIndexId??>
        modelBuilder.lastIndexId(${schema.lastIndexId?c});
</#if>

        EntityBuilder entityBuilder;

<#list schema.entities as entity>
        entityBuilder = modelBuilder.entity("${entity.dbName}");
<#if entity.modelId??>
        entityBuilder.id(${entity.modelId?c})<#if entity.modelUid??>.uid(${entity.modelUid?c}L)</#if><#if
            entity.lastPropertyId??>.lastPropertyId(${entity.lastPropertyId.id?c})</#if>;
</#if>
<#list entity.propertiesColumns as property>
<#assign flags = []>
<#if property.primaryKey><#assign flags = flags + ["PropertyFlags.ID"]></#if>
<#if property.idAssignable><#assign flags = flags + ["PropertyFlags.ID_SELF_ASSIGNABLE"]></#if>
<#if property.notNull><#assign flags = flags + ["PropertyFlags.NOT_NULL"]></#if>
<#if property.nonPrimitiveType && property.propertyType.scalar><#assign flags = flags + ["PropertyFlags.NON_PRIMITIVE_TYPE"]></#if>
<#if property.index??><#assign flags = flags + ["PropertyFlags.INDEXED"]></#if>
<#if property.propertyType == "RelationId"><#assign flags = flags + ["PropertyFlags.INDEXED", "PropertyFlags.INDEX_PARTIAL_SKIP_ZERO"]></#if>
<#assign uniqueFlags = []>
<#list flags as flag>
    <#if !uniqueFlags?seq_contains(flag)><#assign uniqueFlags = uniqueFlags + [flag]></#if>
</#list>
        entityBuilder.property("${property.dbName}", <#--
        --><#if property.targetEntity??>"${property.targetEntity.dbName}", </#if>PropertyType.${property.dbType})<#--
        --><#if property.modelId??>.id(${property.modelId?c})</#if><#--
        --><#if property.modelUid??>.uid(${property.modelUid?c}L)</#if><#--
        --><#if (uniqueFlags?size > 0)>

            .flags(${uniqueFlags?join(" | ")})</#if><#--
        --><#if property.modelIndexId??>.indexId(${property.modelIndexId?c})</#if>;
</#list>
        entityBuilder.entityDone();

</#list>
        return modelBuilder.build();
    }

}
