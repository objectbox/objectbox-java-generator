<#--

Copyright (C) 2018 ObjectBox Ltd.

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
namespace ${schema.defaultJavaPackage};

<#list schema.entities as entity>
table ${entity.dbName} {
<#list entity.propertiesColumns as property>
    ${property.propertyName}:${property.javaType}; <#if property.modelId??>// ID = ${property.modelId}</#if>
</#list>
}

</#list>
