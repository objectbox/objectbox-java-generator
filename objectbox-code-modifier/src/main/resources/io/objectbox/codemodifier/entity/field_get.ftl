<#-- @ftlvariable name="variable" type="io.objectbox.codemodifier.Variable" -->
public ${variable.type.originalName} get${variable.name?cap_first}() {
    return ${variable.name};
}