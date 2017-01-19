/**
 * Removes entity from its object box. Entity must attached to an entity context.
 */
@Generated(hash = GENERATED_HASH_STUB)
public void remove() {
    if (__boxStore == null) {
        throw new DbDetachedException();
    }
    __boxStore.boxFor(${entity.className}.class).remove(this);
}