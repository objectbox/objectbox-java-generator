<#-- @ftlvariable name="entity" type="io.objectbox.generator.model.Entity" -->
<#-- @ftlvariable name="toOne" type="io.objectbox.generator.model.ToOne" -->
/** See {@link io.objectbox.relation.ToOne} for details. */
@Generated(hash = GENERATED_HASH_STUB)
public synchronized ToOne<${toOne.targetEntity.className}> get${toOne.name?cap_first}__toOne() {
    if (${toOne.name}__toOne == null) {
        ${toOne.name}__toOne = new ToOne<>(this, ${entity.className}_.${toOne.fkProperties[0].propertyName}, ${toOne.targetEntity.className}.class);
    }
    return ${toOne.name}__toOne;
}