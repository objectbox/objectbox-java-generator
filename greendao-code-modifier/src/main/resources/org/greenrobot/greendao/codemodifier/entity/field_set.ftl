<#-- @ftlvariable name="variable" type="org.greenrobot.greendao.codemodifier.Variable" -->
public void set${variable.name?cap_first}(${variable.type.originalName} ${variable.name}) {
    this.${variable.name} = ${variable.name};
}