/**
 * Convenient call for {@link de.greenrobot.dao.AbstractDao#delete(Object)}.
 * Entity must attached to an entity context.
 */
@Generated
public void delete() {
    if (myDao == null) {
       throw new DaoException("Entity is detached from DAO context");
    }
    myDao.delete(this);
}