<#-- @ftlvariable name="entity" type="org.greenrobot.greendao.generator.Entity" -->
/** called by internal mechanisms, do not call yourself. */
@Generated(hash = GENERATED_HASH_STUB)
@Internal
public void __setBoxStore(${entity.schema.prefix}BoxStore boxStore) {
    this.__boxStore = boxStore;
    __myBox = boxStore != null ? boxStore.boxFor(${entity.className}.class) : null;
}