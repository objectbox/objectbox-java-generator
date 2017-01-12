package io.objectbox.codemodifier.test;

import io.objectbox.annotation.Entity;
import io.objectbox.annotation.Id;
import io.objectbox.annotation.Relation;

import java.util.List;
import io.objectbox.annotation.Generated;
import io.objectbox.exception.DbException;

@Entity
public class Customer {

    @Id
    long id;

    String name;

    @Relation(idProperty = "customerId")
    List<Order> orders;

    /** Used to resolve relations */
    @Generated(hash = 506680373)
    private transient BoxStore __boxStore;

    /** Used for active entity operations. */
    @Generated(hash = 1448445044)
    private transient Box<Customer> __myBox;

    @Generated(hash = 1039711609)
    public Customer(long id, String name) {
        this.id = id;
        this.name = name;
    }

    @Generated(hash = 60841032)
    public Customer() {
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    /**
     * To-many relationship, resolved on first access (and after reset).
     * Changes to to-many relations are not persisted, make changes to the target entity.
     */
    @Generated(hash = 1019977689)
    public List<Order> getOrders() {
        if (orders == null) {
            final DaoSession daoSession = this.daoSession;
            if (daoSession == null) {
                throw new DaoException("Entity is detached from DAO context");
            }
            OrderCursor targetDao = daoSession.getOrderCursor();
            List<Order> ordersNew = targetDao._queryCustomer_Orders(id);
            synchronized (this) {
                if (orders == null) {
                    orders = ordersNew;
                }
            }
        }
        return orders;
    }

    /** Resets a to-many relationship, making the next get call to query for a fresh result. */
    @Generated(hash = 1446109810)
    public synchronized void resetOrders() {
        orders = null;
    }

    /**
     * Convenient call for {@link org.greenrobot.greendao.AbstractDao#delete(Object)}.
     * Entity must attached to an entity context.
     */
    @Generated(hash = 128553479)
    public void delete() {
        if (myDao == null) {
            throw new DaoException("Entity is detached from DAO context");
        }
        myDao.delete(this);
    }

    /**
     * Convenient call for {@link org.greenrobot.greendao.AbstractDao#refresh(Object)}.
     * Entity must attached to an entity context.
     */
    @Generated(hash = 1942392019)
    public void refresh() {
        if (myDao == null) {
            throw new DaoException("Entity is detached from DAO context");
        }
        myDao.refresh(this);
    }

    /**
     * Convenient call for {@link org.greenrobot.greendao.AbstractDao#update(Object)}.
     * Entity must attached to an entity context.
     */
    @Generated(hash = 713229351)
    public void update() {
        if (myDao == null) {
            throw new DaoException("Entity is detached from DAO context");
        }
        myDao.update(this);
    }

    /** called by internal mechanisms, do not call yourself. */
    @Generated(hash = 816544730)
    @Internal
    public void __setBoxStore(BoxStore boxStore) {
        this.__boxStore = boxStore;
        __myBox = boxStore != null ? boxStore.boxFor(Customer.class) : null;
    }
}
