/**
 * Puts the entity in its object box.
 * Entity must attached to an entity context.
 */
@Generated(hash = GENERATED_HASH_STUB)
public void put() {
    if (__myBox == null) {
        throw new DbDetachedException();
    }
    __myBox.put(this);
}