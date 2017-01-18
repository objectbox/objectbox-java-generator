<#-- @ftlvariable name="toOne" type="org.greenrobot.greendao.generator.ToOne" -->
<#-- @ftlvariable name="notNullAnnotation" type="java.lang.String" -->
/** Set the to-one relation including its ID property. */
@Generated(hash = GENERATED_HASH_STUB)
public void set${toOne.name?cap_first}(<#if false && toOne.fkProperties[0].notNull>${notNullAnnotation} </#if>${toOne.targetEntity.className} ${toOne.name}) {
<#if false && toOne.fkProperties[0].notNull>
    if (${toOne.name} == null) {
        throw new DbException("To-one property '${toOne.fkProperties[0].propertyName}' has not-null constraint; cannot set to-one to null");
    }
</#if>
    synchronized (this) {
        this.${toOne.name} = ${toOne.name};
<#if toOne.useFkProperty>
        ${toOne.fkProperties[0].propertyName} = <#if true || !toOne.fkProperties[0].notNull>${toOne.name} == null ? 0 : </#if>${toOne.name}.get${toOne.targetEntity.pkProperty.propertyName?cap_first}();
        ${toOne.name}__resolvedKey = ${toOne.fkProperties[0].propertyName};
<#else>
        ${toOne.name}__refreshed = true;
</#if>
    }
}