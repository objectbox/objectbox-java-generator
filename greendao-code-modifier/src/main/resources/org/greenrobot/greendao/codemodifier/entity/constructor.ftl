<#-- @ftlvariable name="className" type="java.lang.String" -->
<#-- @ftlvariable name="fields" type="java.util.List<org.greenrobot.greendao.codemodifier.EntityField>" -->
<#-- @ftlvariable name="notNullAnnotation" type="java.lang.String" -->
@Generated
public ${className}(<#list fields as f><#if f.notNull && !f.variable.type.primitive>${notNullAnnotation} </#if>${f.variable.type.originalName} ${f.variable.name}<#sep>, </#list>) {
<#list fields as f>
    this.${f.variable.name} = ${f.variable.name};
</#list>
}
