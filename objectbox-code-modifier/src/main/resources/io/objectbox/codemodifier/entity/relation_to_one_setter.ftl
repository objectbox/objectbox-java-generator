<#-- @ftlvariable name="toOne" type="io.objectbox.generator.model.ToOne" -->
<#-- @ftlvariable name="notNullAnnotation" type="java.lang.String" -->
/** Set the to-one relation including its ID property. */
@Generated(GENERATED_HASH_STUB)
public void set${toOne.name?cap_first}(<#if false && toOne.targetIdProperty.notNull>${notNullAnnotation} </#if>${toOne.targetEntity.className} ${toOne.name}) {
    ${toOne.nameToOne}.setTarget(${toOne.name});
    this.${toOne.name} = ${toOne.name};
}