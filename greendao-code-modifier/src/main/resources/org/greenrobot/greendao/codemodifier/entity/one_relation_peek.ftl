<#-- @ftlvariable name="toOne" type="de.greenrobot.daogenerator.ToOne" -->
/** To-one relationship, returned entity is not refreshed and may carry only the PK property. */
@Generated
public ${toOne.targetEntity.className} peak${toOne.name?cap_first}() {
    return ${toOne.name};
}