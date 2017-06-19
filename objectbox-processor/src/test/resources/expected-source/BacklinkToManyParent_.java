package io.objectbox.processor.test;

import io.objectbox.processor.test.BacklinkToManyParentCursor.Factory;

import io.objectbox.EntityInfo;
import io.objectbox.Property;
import io.objectbox.annotation.apihint.Internal;
import io.objectbox.internal.CursorFactory;
import io.objectbox.internal.IdGetter;
import io.objectbox.relation.RelationInfo;
import io.objectbox.relation.ToOne;
import io.objectbox.relation.ToOneGetter;

//////
// NOTE: this is the EXPECTED generated source.
//////

/**
 * Properties for entity "BacklinkToManyParent". Can be used for QueryBuilder and for referencing DB names.
 */
public final class BacklinkToManyParent_ implements EntityInfo<BacklinkToManyParent> {

    // Leading underscores for static constants to avoid naming conflicts with property names

    public static final String __ENTITY_NAME = "BacklinkToManyParent";

    public static final int __ENTITY_ID = 1;

    public static final Class<BacklinkToManyParent> __ENTITY_CLASS = BacklinkToManyParent.class;

    public static final String __DB_NAME = "BacklinkToManyParent";

    public static final CursorFactory<BacklinkToManyParent> __CURSOR_FACTORY = new Factory();

    @Internal
    static final BacklinkToManyParentIdGetter __ID_GETTER = new BacklinkToManyParentIdGetter();

    public final static Property id = new Property(0, 1, Long.class, "id", true, "id");

    public final static Property[] __ALL_PROPERTIES = {
            id
    };

    public final static Property __ID_PROPERTY = id;

    public final static BacklinkToManyParent_ __INSTANCE = new BacklinkToManyParent_();

    @Override
    public String getEntityName() {
        return __ENTITY_NAME;
    }

    @Override
    public int getEntityId() {
        return __ENTITY_ID;
    }

    @Override
    public Class<BacklinkToManyParent> getEntityClass() {
        return __ENTITY_CLASS;
    }

    @Override
    public String getDbName() {
        return __DB_NAME;
    }

    @Override
    public Property[] getAllProperties() {
        return __ALL_PROPERTIES;
    }

    @Override
    public Property getIdProperty() {
        return __ID_PROPERTY;
    }

    @Override
    public IdGetter<BacklinkToManyParent> getIdGetter() {
        return __ID_GETTER;
    }

    @Override
    public CursorFactory<BacklinkToManyParent> getCursorFactory() {
        return __CURSOR_FACTORY;
    }

    @Internal
    static final class BacklinkToManyParentIdGetter implements IdGetter<BacklinkToManyParent> {
        public long getId(BacklinkToManyParent object) {
            Long id = object.id;
            return id != null? id : 0;
        }
    }

    static final RelationInfo<BacklinkToManyChild> children =
            new RelationInfo<>(BacklinkToManyParent_.__INSTANCE, BacklinkToManyChild_.__INSTANCE, BacklinkToManyChild_.parentId, new ToOneGetter<BacklinkToManyChild>() {
                @Override
                public ToOne<BacklinkToManyParent> getToOne(BacklinkToManyChild entity) {
                    return entity.parent;
                }
            });

}
