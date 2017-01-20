<#-- @ftlvariable name="entity" type="org.greenrobot.greendao.generator.Entity" -->
<#-- @ftlvariable name="toOne" type="org.greenrobot.greendao.generator.ToOne" -->
/** To-one relationship, resolved on first access. */
@Generated(hash = GENERATED_HASH_STUB)
public ${toOne.targetEntity.className} get${toOne.name?cap_first}() {
    ${toOne.name} = get${toOne.name?cap_first}__toOne().getTarget(this.${toOne.fkProperties[0].propertyName});
    return ${toOne.name};
}