<#-- @ftlvariable name="toMany" type="io.objectbox.generator.model.ToManyBase" -->
<#-- @ftlvariable name="entity" type="io.objectbox.generator.model.Entity" -->
/**
 * To-many relationship, resolved on first access (and after reset).
 * Changes to to-many relations are not persisted, make changes to the target entity.
 */
@Generated(hash = GENERATED_HASH_STUB)
public List<${toMany.targetEntity.className}> get${toMany.name?cap_first}() {
    if (${toMany.name} == null) {
        final BoxStore boxStore = this.__boxStore;
        if (boxStore == null) {
            throw new DbDetachedException();
        }
        Box<${toMany.targetEntity.className}> box = boxStore.boxFor(${toMany.targetEntity.className}.class);
        int targetTypeId = boxStore.getEntityTypeIdOrThrow(${toMany.targetEntity.className}.class);
        List<${toMany.targetEntity.className}> ${toMany.name}New = box.getBacklinkEntities(targetTypeId,<#--
         -->${toMany.targetEntity.className}_.${toMany.targetProperties[0].propertyName}, ${entity.pkProperty.propertyName});
        synchronized (this) {<#-- Check if another thread was faster, we cannot lock while doing the query to prevent deadlocks -->
            if(${toMany.name} == null) {
                ${toMany.name} = ${toMany.name}New;
            }
        }
    }
    return ${toMany.name};
}