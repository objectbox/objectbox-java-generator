<#--

Copyright (C) 2016-2017 ObjectBox Ltd.
                                                                           
This file is part of greenDAO Generator.                                   
                                                                           
greenDAO Generator is free software: you can redistribute it and/or modify 
it under the terms of the GNU General Public License as published by       
the Free Software Foundation, either version 3 of the License, or          
(at your option) any later version.                                        
greenDAO Generator is distributed in the hope that it will be useful,      
but WITHOUT ANY WARRANTY; without even the implied warranty of             
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the              
GNU General Public License for more details.                               
                                                                           
You should have received a copy of the GNU General Public License          
along with greenDAO Generator.  If not, see <http://www.gnu.org/licenses/>.

-->
<#-- @ftlvariable name="entity" type="io.objectbox.generator.model.Entity" -->
<#-- @ftlvariable name="schema" type="io.objectbox.generator.model.Schema" -->

<#assign daoCompatPackage = "org.greenrobot.daocompat" >
<#assign toBindType = {"Boolean":"Long", "Byte":"Long", "Short":"Long", "Int":"Long", "Long":"Long", "Float":"Double", "Double":"Double", "String":"String", "ByteArray":"Blob", "Date": "Long" } />
<#assign toCursorType = {"Boolean":"Short", "Byte":"Short", "Short":"Short", "Int":"Int", "Long":"Long", "Float":"Float", "Double":"Double", "String":"String", "ByteArray":"Blob", "Date": "Long"  } />
package ${entity.javaPackageDao};

<#if entity.toOneRelations?has_content || entity.incomingToManyRelations?has_content>
import java.util.List;
</#if>

import io.objectbox.Box;
import io.objectbox.Property;
import ${daoCompatPackage}.AbstractDao;
import ${daoCompatPackage}.identityscope.IdentityScopeLong;
<#if entity.incomingToManyRelations?has_content>
import ${daoCompatPackage}.query.Query;
import ${daoCompatPackage}.query.QueryBuilder;
</#if>

<#if entity.javaPackageDao != schema.defaultJavaPackageDao>
import ${schema.defaultJavaPackageDao}.${schema.prefix}DaoSession;

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
// THIS CODE IS GENERATED BY objectbox, DO NOT EDIT.
/** 
 * DAO for table "${entity.dbName}".
*/
public class ${entity.classNameDao} extends AbstractDao<${entity.className}, Long> {

    public static final String TABLENAME = "${entity.dbName}";

    /**
    * Properties of the ${entity.className} box.
    */
    private static io.objectbox.EntityInfo BOX_PROPERTIES = new ${entity.className}_();

    /**
     * Properties of entity ${entity.className}.<br/>
     * Can be used for QueryBuilder and for referencing column names.
     */
    public static class Properties {
<#list entity.propertiesColumns as property>
        public final static Property ${property.propertyName?cap_first} = ${entity.className}_.${property.propertyName};
</#list>
    }

<#list entity.properties as property><#if property.customType?has_content><#--
-->    private final ${property.converterClassName} ${property.propertyName}Converter = new ${property.converterClassName}();
</#if></#list>
<#list entity.incomingToManyRelations as toMany>
    private Query<${toMany.targetEntity.className}> ${toMany.sourceEntity.className?uncap_first}_${toMany.name?cap_first}Query;
</#list>

    public ${entity.classNameDao}(Box<${entity.className}> box, IdentityScopeLong<${entity.className}> identityScope) {
        super(box, BOX_PROPERTIES, identityScope);
    }
    
    public ${entity.classNameDao}(${schema.prefix}DaoSession daoSession, Box<${entity.className}> box, IdentityScopeLong<${entity.className}> identityScope) {
        super(daoSession, box, BOX_PROPERTIES, identityScope);
    }

    @Override
    public void readEntity(${entity.className} from, ${entity.className} to) {
<#if entity.protobuf>
        throw new UnsupportedOperationException("Protobuf objects cannot be modified");
<#else>
<#list entity.properties as property>
        to.set${property.propertyName?cap_first}(from.get${property.propertyName?cap_first}());
</#list>
</#if>
     }
    
    @Override
    public ${entity.pkType} getKey(${entity.className} entity) {
<#if entity.pkProperty??>
        if(entity != null) {
            return entity.get${entity.pkProperty.propertyName?cap_first}();
        } else {
            return null;
        }
<#else>
        return null;
</#if>    
    }

    @Override
    protected final boolean isEntityUpdateable() {
        return ${(!entity.protobuf)?string};
    }

}
