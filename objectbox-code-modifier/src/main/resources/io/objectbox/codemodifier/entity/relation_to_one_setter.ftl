<#-- @ftlvariable name="toOne" type="io.objectbox.generator.model.ToOne" -->
<#-- @ftlvariable name="notNullAnnotation" type="java.lang.String" -->
/** Set the to-one relation including its ID property. */
@Generated(hash = GENERATED_HASH_STUB)
public void set${toOne.name?cap_first}(<#if false && toOne.targetIdProperty.notNull>${notNullAnnotation} </#if>${toOne.targetEntity.className} ${toOne.name}) {
<#if false && toOne.targetIdProperty.notNull>
    if (${toOne.name} == null) {
        throw new DbException("To-one property '${toOne.targetIdProperty.propertyName}' has not-null constraint; cannot set to-one to null");
    }
</#if>
    ${toOne.name}__toOne.setTarget(${toOne.name});
    this.${toOne.name} = ${toOne.name};
}