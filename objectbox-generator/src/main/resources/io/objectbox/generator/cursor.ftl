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
<#-- @ftlvariable name="entity" type="io.objectbox.generator.model.Entity" -->
<#-- @ftlvariable name="schema" type="io.objectbox.generator.model.Schema" -->
<#-- @ftlvariable name="propertyCollector" type="java.lang.String" -->

<#assign toBindType = {"Boolean":"Long", "Byte":"Long", "Short":"Long", "Int":"Long", "Long":"Long", "Float":"Double", "Double":"Double", "String":"String", "ByteArray":"Blob", "Date": "Long" } />
<#assign toCursorType = {"Boolean":"Short", "Byte":"Short", "Short":"Short", "Int":"Int", "Long":"Long", "Float":"Float", "Double":"Double", "String":"String", "ByteArray":"Blob", "Date": "Long"  } />
package ${entity.javaPackageDao};

<#if entity.toOneRelations?has_content || entity.incomingToManyRelations?has_content>
import java.util.List;
</#if>
<#if entity.toOneRelations?has_content>
import java.util.ArrayList;
</#if>

import io.objectbox.Cursor;
import io.objectbox.Properties;
import io.objectbox.Transaction;
import io.objectbox.annotation.apihint.Temporary;
<#if entity.toOneRelations?has_content>
</#if>
<#if entity.incomingToManyRelations?has_content>
<#-- TODO
import io.objectbox.query.Query;
import io.objectbox.query.QueryBuilder;
-->
</#if>

<#if entity.javaPackageDao != schema.defaultJavaPackageDao>
//import ${schema.defaultJavaPackageDao}.${schema.prefix}DaoSession;

</#if>
<#if entity.additionalImportsDao?has_content>
<#list entity.additionalImportsDao as additionalImport>
import ${additionalImport};
</#list>

</#if>
<#if entity.javaPackageDao != entity.javaPackage>
import ${entity.javaPackage}.${entity.className};

</#if>
<#if entity.protobuf>
import ${entity.javaPackage}.${entity.className}.Builder;

</#if>
// THIS CODE IS GENERATED BY ObjectBox, DO NOT EDIT.

/**
 * Cursor for DB entity "${entity.dbName}".
 */
public final class ${entity.classNameDao} extends Cursor<${entity.className}> {

    private static final Properties PROPERTIES = new ${entity.className}_();

    private static final ${entity.className}IdGetter ID_GETTER = PROPERTIES.__ID_GETTER;

<#list entity.properties as property><#if property.customType?has_content><#--
-->    private final ${property.converterClassName} ${property.propertyName}Converter = new ${property.converterClassName}();
</#if></#list>
<#list entity.incomingToManyRelations as toMany>
    // TODO private Query<${toMany.targetEntity.className}> ${toMany.sourceEntity.className?uncap_first}_${toMany.name?cap_first}Query;
</#list>

    // Property IDs get verified in Cursor base class
    private final static int __ID_${entity.pkProperty.propertyName} = ${entity.className}_.${entity.pkProperty.propertyName}.id;

    public ${entity.classNameDao}(Transaction tx, long cursor) {
        super(tx, cursor, PROPERTIES);
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
        entity.get${toOne.name?cap_first}__toOne().internalPrepareForPut(boxStoreForEntities);
</#list>
${propertyCollector}
    }

<#if entity.active>
    // TODO @Override
    protected final void attachEntity(${entity.className} entity) {
        // TODO super.attachEntity(entity);
        //entity.__boxStore = boxStoreForEntities;
    }

</#if>
    // TODO do we need this? @Override
    protected final boolean isEntityUpdateable() {
        return ${(!entity.protobuf)?string};
    }

<#list entity.incomingToManyRelations as toMany>
    /** Internal query to resolve the "${toMany.name}" to-many relationship of ${toMany.sourceEntity.className}. */
    /* TODO
    public List<${toMany.targetEntity.className}> _query${toMany.sourceEntity.className?cap_first}_${toMany.name?cap_first}(<#--
    --><#if toMany.targetProperties??><#list toMany.targetProperties as property><#--
    -->${property.javaType} ${property.propertyName}<#if property_has_next>, </#if></#list><#else><#--
    -->${toMany.sourceProperty.javaType} ${toMany.sourceProperty.propertyName}</#if>) {
        synchronized (this) {
            if (${toMany.sourceEntity.className?uncap_first}_${toMany.name?cap_first}Query == null) {
                QueryBuilder<${toMany.targetEntity.className}> queryBuilder = queryBuilder();
<#if toMany.targetProperties??>
    <#list toMany.targetProperties as property>
                queryBuilder.where(Properties.${property.propertyName}.eq(null));
    </#list>
<#else>
                queryBuilder.join(${toMany.joinEntity.className}.class, ${toMany.joinEntity.classNameDao}.Properties.${toMany.targetProperty.propertyName})
                    .where(${toMany.joinEntity.classNameDao}.Properties.${toMany.sourceProperty.propertyName}.eq(${toMany.sourceProperty.propertyName}));
</#if>
<#if toMany.order?has_content>
                queryBuilder.orderRaw("${toMany.order}");
</#if>
                ${toMany.sourceEntity.className?uncap_first}_${toMany.name?cap_first}Query = queryBuilder.build();
            }
        }
        Query<${toMany.targetEntity.className}> query = ${toMany.sourceEntity.className?uncap_first}_${toMany.name?cap_first}Query.forCurrentThread();
<#if toMany.targetProperties??>
    <#list toMany.targetProperties as property>
        query.setParameter(${property_index}, ${property.propertyName});
    </#list>
<#else>
        query.setParameter(0, ${toMany.sourceProperty.propertyName});
</#if>
        return query.list();
    }
    */

</#list>   
<#if entity.toOneRelations?has_content>
<#-- TODO replace include "dao-deep.ftl" -->
</#if>
}
