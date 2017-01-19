/**
 * Puts the entity in its object box.
 * Entity must attached to an entity context.
 */
@Generated(hash = GENERATED_HASH_STUB)
public void put() {
    if (__boxStore == null) {
        throw new DbDetachedException();
    }
    __boxStore.boxFor(${entity.className}.class).put(this);
}