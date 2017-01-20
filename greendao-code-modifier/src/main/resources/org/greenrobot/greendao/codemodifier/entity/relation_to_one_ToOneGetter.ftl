<#-- @ftlvariable name="entity" type="org.greenrobot.greendao.generator.Entity" -->
<#-- @ftlvariable name="toOne" type="org.greenrobot.greendao.generator.ToOne" -->
/** See {@link io.objectbox.relation.ToOne} for details. */
@Generated(hash = GENERATED_HASH_STUB)
public synchronized ToOne<${entity.className}, ${toOne.targetEntity.className}> get${toOne.name?cap_first}__toOne() {
    if (${toOne.name}__toOne == null) {
        ${toOne.name}__toOne = new ToOne<>(this, ${entity.className}_.${toOne.fkProperties[0].propertyName}, ${toOne.targetEntity.className}.class);
    }
    return ${toOne.name}__toOne;
}