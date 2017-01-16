/**
 * Removes entity from its object box. Entity must attached to an entity context.
 */
@Generated(hash = GENERATED_HASH_STUB)
public void remove() {
    if (__myBox == null) {
        throw new DbDetachedException();
    }
    __myBox.remove(this);
}