package io.objectbox.codemodifier.test;

import io.objectbox.annotation.Entity;
import io.objectbox.annotation.Generated;
import io.objectbox.annotation.Id;
import io.objectbox.annotation.Relation;

import java.util.List;
import io.objectbox.exception.DbException;
import io.objectbox.annotation.NotNull;

@Entity
public class Order {

    @Id
    long id;

    long customerId;

    @Relation()
    Customer customer;

    /** Used to resolve relations */
    @Generated(hash = 506680373)
    private transient BoxStore __boxStore;

    /** Used for active entity operations. */
    @Generated(hash = 1538225439)
    private transient Box<Order> __myBox;

    @Generated(hash = 789963847)
    public Order(long id, long customerId) {
        this.id = id;
        this.customerId = customerId;
    }

    @Generated(hash = 1105174599)
    public Order() {
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public long getCustomerId() {
        return customerId;
    }

    public void setCustomerId(long customerId) {
        this.customerId = customerId;
    }

    @Generated(hash = 8592637)
    private transient Long customer__resolvedKey;

    /** To-one relationship, resolved on first access. */
    @Generated(hash = 282427793)
    public Customer getCustomer() {
        long __key = this.customerId;
        if (customer__resolvedKey == null || !customer__resolvedKey.equals(__key)) {
            final BoxStore boxStore = this.__boxStore;
            if (boxStore == null) {
                throw new DbException("Entity is detached from box");
            }
            Box<Customer> box = boxStore.boxFor(Customer.class);
            Customer customerNew = box.get(__key);
            synchronized (this) {
                customer = customerNew;
                customer__resolvedKey = __key;
            }
        }
        return customer;
    }

    /** called by internal mechanisms, do not call yourself. */
    @Generated(hash = 625323961)
    public void setCustomer(@NotNull Customer customer) {
        if (customer == null) {
            throw new DaoException("To-one property 'customerId' has not-null constraint; cannot set to-one to null");
        }
        synchronized (this) {
            this.customer = customer;
            customerId = customer.getId();
            customer__resolvedKey = customerId;
        }
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
    @Generated(hash = 1546418180)
    @Internal
    public void __setBoxStore(BoxStore boxStore) {
        this.__boxStore = boxStore;
        __myBox = boxStore != null ? boxStore.boxFor(Order.class) : null;
    }

}
