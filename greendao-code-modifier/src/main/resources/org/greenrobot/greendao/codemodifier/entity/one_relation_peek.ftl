<#-- @ftlvariable name="toOne" type="org.greenrobot.greendao.generator.ToOne" -->
/** To-one relationship, returned entity is not refreshed and may carry only the PK property. */
@Generated(hash = GENERATED_HASH_STUB)
public ${toOne.targetEntity.className} peak${toOne.name?cap_first}() {
    return ${toOne.name};
}