/**
 * Convenient call for {@link de.greenrobot.dao.AbstractDao#refresh(Object)}.
 * Entity must attached to an entity context.
 */
@Generated
public void refresh() {
    if (myDao == null) {
        throw new DaoException("Entity is detached from DAO context");
    }
    myDao.refresh(this);
}