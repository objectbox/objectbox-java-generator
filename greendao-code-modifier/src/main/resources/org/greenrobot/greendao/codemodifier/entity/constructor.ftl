<#-- @ftlvariable name="className" type="java.lang.String" -->
<#-- @ftlvariable name="properties" type="java.util.List<org.greenrobot.greendao.codemodifier.ParsedProperty>" -->
<#-- @ftlvariable name="notNullAnnotation" type="java.lang.String" -->
@Generated(hash = GENERATED_HASH_STUB)
public ${className}(<#list properties as property><#if property.notNull && !property.variable.type.primitive>${notNullAnnotation} </#if>${property.variable.type.originalName} ${property.variable.name}<#sep>, </#list>) {
<#list properties as property>
    this.${property.variable.name} = ${property.variable.name};
</#list>
}