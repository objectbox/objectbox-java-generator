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
<#assign daoCompatPackage = "org.greenrobot.daocompat" >
package ${schema.defaultJavaPackageDao};

import io.objectbox.BoxStore;
import ${daoCompatPackage}.AbstractDaoSession;
import ${daoCompatPackage}.identityscope.IdentityScopeLong;

<#list schema.entities as entity>
import ${entity.javaPackage}.${entity.className};
</#list>

<#list schema.entities as entity>
import ${entity.javaPackageDao}.${entity.classNameDao};
</#list>

// THIS CODE IS GENERATED BY ObjectBox, DO NOT EDIT.

/**
 * {@inheritDoc}
 * 
 * @see ${daoCompatPackage}.AbstractDaoSession
 */
public class ${schema.prefix}DaoSession extends AbstractDaoSession {

<#list schema.entities as entity>
    private final IdentityScopeLong<${entity.className}> ${entity.classNameDao?uncap_first}IdentityScope;
</#list>

<#list schema.entities as entity>
    private final ${entity.classNameDao} ${entity.classNameDao?uncap_first};
</#list>        

    public ${schema.prefix}DaoSession(BoxStore store) {
        super(store);

<#list schema.entities as entity>
        ${entity.classNameDao?uncap_first}IdentityScope = new IdentityScopeLong<>();
</#list>

<#list schema.entities as entity>
        ${entity.classNameDao?uncap_first} = new ${entity.classNameDao}<#--
-->(this, store.boxFor(${entity.className}.class), ${entity.classNameDao?uncap_first}IdentityScope);
</#list>        

<#list schema.entities as entity>
        registerDao(${entity.className}.class, ${entity.classNameDao?uncap_first});
</#list>        
    }
    
    public void clear() {
<#list schema.entities as entity>
        ${entity.classNameDao?uncap_first}IdentityScope.clear();
</#list>
    }

<#list schema.entities as entity>
    public ${entity.classNameDao} get${entity.classNameDao?cap_first}() {
        return ${entity.classNameDao?uncap_first};
    }

</#list>        
}
