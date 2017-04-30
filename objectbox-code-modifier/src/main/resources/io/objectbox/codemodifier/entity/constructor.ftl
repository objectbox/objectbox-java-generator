<#-- @ftlvariable name="className" type="java.lang.String" -->
<#-- @ftlvariable name="properties" type="java.util.List<io.objectbox.codemodifier.ParsedProperty>" -->
<#-- @ftlvariable name="notNullAnnotation" type="java.lang.String" -->
@Generated(GENERATED_HASH_STUB)
@Internal
/** This constructor was generated by ObjectBox and may change any time. */
public ${className}(<#list properties as property><#if property.notNull && !property.variable.type.primitive>${notNullAnnotation} </#if>${property.variable.type.originalName} ${property.variable.name}<#sep>, </#list>) {
<#list properties as property>
    <#if property.virtualTargetName??>
    this.${property.virtualTargetName}.setTargetId(${property.variable.name});
    <#else>
    this.${property.variable.name} = ${property.variable.name};
    </#if>
</#list>
}