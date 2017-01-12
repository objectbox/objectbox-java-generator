<#-- @ftlvariable name="entity" type="org.greenrobot.greendao.generator.Entity" -->
<#-- @ftlvariable name="toOne" type="org.greenrobot.greendao.generator.ToOne" -->
/** To-one relationship, resolved on first access. */
@Generated(hash = GENERATED_HASH_STUB)
public ${toOne.targetEntity.className} get${toOne.name?cap_first}() {
<#if toOne.useFkProperty>
${toOne.fkProperties[0].javaType} __key = this.${toOne.fkProperties[0].propertyName};
    if (${toOne.name}__resolvedKey == null || <#--
--><#if toOne.resolvedKeyUseEquals[0]>!${toOne.name}__resolvedKey.equals(__key)<#--
--><#else>${toOne.name}__resolvedKey != __key</#if>) {
        final ${entity.schema.prefix}BoxStore boxStore = this.boxStore;
        if (boxStore == null) {
            throw new DbException("Entity is detached from box");
        }
        Box<${toOne.targetEntity.className}> box = boxStore.boxFor(${toOne.targetEntity.className}.class);
        ${toOne.targetEntity.className} ${toOne.name}New = box.get(__key);
        synchronized (this) {
            ${toOne.name} = ${toOne.name}New;
            ${toOne.name}__resolvedKey = __key;
        }
    }
<#else>
    // XXX Unsupported! This case may be deleted in the future
    if (${toOne.name} != null || !${toOne.name}__refreshed) {
        if (daoSession == null) {
            throw new DaoException("Entity is detached from DAO context");
        }
        ${toOne.targetEntity.classNameDao} targetDao = daoSession.get${toOne.targetEntity.classNameDao?cap_first}();
        targetDao.refresh(${toOne.name});
        ${toOne.name}__refreshed = true;
    }
</#if>
    return ${toOne.name};
}