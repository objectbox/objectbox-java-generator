/**
 * Convenient call for {@link org.greenrobot.greendao.AbstractDao#refresh(Object)}.
 * Entity must attached to an entity context.
 */
@Generated(hash = GENERATED_HASH_STUB)
public void refresh() {
    if (__myBox == null) {
        throw new DbDetachedException();
    }
    __myBox.refresh(this);
}