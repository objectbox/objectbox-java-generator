/**
 * Convenient call for {@link org.greenrobot.greendao.AbstractDao#delete(Object)}.
 * Entity must attached to an entity context.
 */
@Generated(hash = GENERATED_HASH_STUB)
public void delete() {
    if (myDao == null) {
       throw new DaoException("Entity is detached from DAO context");
    }
    myDao.delete(this);
}