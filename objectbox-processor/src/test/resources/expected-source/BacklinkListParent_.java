package io.objectbox.processor.test;

import io.objectbox.processor.test.BacklinkListParentCursor.Factory;

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
 * Properties for entity "BacklinkListParent". Can be used for QueryBuilder and for referencing DB names.
 */
public final class BacklinkListParent_ implements EntityInfo<BacklinkListParent> {

    // Leading underscores for static constants to avoid naming conflicts with property names

    public static final String __ENTITY_NAME = "BacklinkListParent";

    public static final int __ENTITY_ID = 1;

    public static final Class<BacklinkListParent> __ENTITY_CLASS = BacklinkListParent.class;

    public static final String __DB_NAME = "BacklinkListParent";

    public static final CursorFactory<BacklinkListParent> __CURSOR_FACTORY = new Factory();

    @Internal
    static final BacklinkListParentIdGetter __ID_GETTER = new BacklinkListParentIdGetter();

    public final static Property id = new Property(0, 1, Long.class, "id", true, "id");

    public final static Property[] __ALL_PROPERTIES = {
            id
    };

    public final static Property __ID_PROPERTY = id;

    public final static BacklinkListParent_ __INSTANCE = new BacklinkListParent_();

    @Override
    public String getEntityName() {
        return __ENTITY_NAME;
    }

    @Override
    public int getEntityId() {
        return __ENTITY_ID;
    }

    @Override
    public Class<BacklinkListParent> getEntityClass() {
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
    public IdGetter<BacklinkListParent> getIdGetter() {
        return __ID_GETTER;
    }

    @Override
    public CursorFactory<BacklinkListParent> getCursorFactory() {
        return __CURSOR_FACTORY;
    }

    @Internal
    static final class BacklinkListParentIdGetter implements IdGetter<BacklinkListParent> {
        @Override
        public long getId(BacklinkListParent object) {
            Long id = object.id;
            return id != null? id : 0;
        }
    }

    static final RelationInfo<BacklinkListChild> children =
            new RelationInfo<>(BacklinkListParent_.__INSTANCE, BacklinkListChild_.__INSTANCE, BacklinkListChild_.parentId, new ToOneGetter<BacklinkListChild>() {
                @Override
                public ToOne<BacklinkListParent> getToOne(BacklinkListChild entity) {
                    return entity.parent;
                }
            });

}
