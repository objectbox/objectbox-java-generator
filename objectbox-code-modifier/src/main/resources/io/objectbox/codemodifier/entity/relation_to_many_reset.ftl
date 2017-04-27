<#-- @ftlvariable name="toMany" type="io.objectbox.generator.model.ToManyBase" -->
/** Resets a to-many relationship, making the next get call to query for a fresh result. */
@Generated(GENERATED_HASH_STUB)
public synchronized void reset${toMany.name?cap_first}() {
    ${toMany.name} = null;
}