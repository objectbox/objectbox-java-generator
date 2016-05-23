<#-- @ftlvariable name="toMany" type="org.greenrobot.greendao.generator.ToManyBase" -->
<#-- @ftlvariable name="entity" type="org.greenrobot.greendao.generator.Entity" -->
/**
 * To-many relationship, resolved on first access (and after reset).
 * Changes to to-many relations are not persisted, make changes to the target entity.
 */
@Generated(hash = GENERATED_HASH_STUB)
public List<${toMany.targetEntity.className}> get${toMany.name?cap_first}() {
    if (${toMany.name} == null) {
        final ${entity.schema.prefix}DaoSession daoSession = this.daoSession;
        if (daoSession == null) {
            throw new DaoException("Entity is detached from DAO context");
        }
        ${toMany.targetEntity.classNameDao} targetDao = daoSession.get${toMany.targetEntity.classNameDao?cap_first}();
        List<${toMany.targetEntity.className}> ${toMany.name}New = targetDao._query${toMany.sourceEntity.className?cap_first}_${toMany.name?cap_first}(<#--
                --><#if toMany.sourceProperties??><#list toMany.sourceProperties as property>${property.propertyName}<#if property_has_next>, </#if></#list><#else><#--
                -->${entity.pkProperty.propertyName}</#if>);
        synchronized (this) {<#-- Check if another thread was faster, we cannot lock while doing the query to prevent deadlocks -->
            if(${toMany.name} == null) {
                ${toMany.name} = ${toMany.name}New;
            }
        }
    }
    return ${toMany.name};
}