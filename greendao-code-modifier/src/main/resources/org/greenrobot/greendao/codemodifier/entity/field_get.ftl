<#-- @ftlvariable name="variable" type="org.greenrobot.greendao.codemodifier.Variable" -->
public ${variable.type.originalName} get${variable.name?cap_first}() {
    return ${variable.name};
}