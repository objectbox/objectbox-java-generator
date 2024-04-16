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
<#-- @ftlvariable name="schema" type="io.objectbox.generator.model.Schema" -->
<#assign toFlatbuffersType = {"Boolean":"bool", "Byte":"byte", "Short":"short", "Int":"int", "Long":"long", "Float":"float", "Double":"double", "String":"string", "ByteArray":"[byte]", "Date": "long"  } />
namespace ${schema.defaultJavaPackage}.fbs;

<#list schema.entities as entity>
table ${entity.dbName} {
<#list entity.propertiesColumns as property>
    <#assign fbsType>${toFlatbuffersType[property.propertyType]!property.javaType}</#assign>
    ${property.propertyName}:${fbsType}; <#if property.modelId??>// ID = ${property.modelId}</#if>
</#list>
}

</#list>
