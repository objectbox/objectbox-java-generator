package io.objectbox.processor.test;

import io.objectbox.processor.test.ToOneChildCursor.Factory;

import io.objectbox.EntityInfo;
import io.objectbox.Property;
import io.objectbox.annotation.apihint.Internal;
import io.objectbox.internal.CursorFactory;
import io.objectbox.internal.IdGetter;
import io.objectbox.relation.RelationInfo;
import io.objectbox.relation.ToOne;
import io.objectbox.internal.ToOneGetter;

//////
// NOTE: this is the EXPECTED generated source.
//////

/**
 * Properties for entity "ToOneChild". Can be used for QueryBuilder and for referencing DB names.
 */
public final class ToOneChild_ implements EntityInfo<ToOneChild> {

    // Leading underscores for static constants to avoid naming conflicts with property names

    public static final String __ENTITY_NAME = "ToOneChild";

    public static final int __ENTITY_ID = 1;

    public static final Class<ToOneChild> __ENTITY_CLASS = ToOneChild.class;

    public static final String __DB_NAME = "ToOneChild";

    public static final CursorFactory<ToOneChild> __CURSOR_FACTORY = new Factory();

    @Internal
    static final ToOneChildIdGetter __ID_GETTER = new ToOneChildIdGetter();

    public final static Property id = new Property(0, 1, Long.class, "id", true, "id");
    public final static Property aParentId = new Property(1, 3, long.class, "aParentId");
    public final static Property parentId = new Property(2, 2, long.class, "parentId");

    public final static Property[] __ALL_PROPERTIES = {
            id,
            aParentId,
            parentId
    };

    public final static Property __ID_PROPERTY = id;

    public final static ToOneChild_ __INSTANCE = new ToOneChild_();

    @Override
    public String getEntityName() {
        return __ENTITY_NAME;
    }

    @Override
    public int getEntityId() {
        return __ENTITY_ID;
    }

    @Override
    public Class<ToOneChild> getEntityClass() {
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
    public IdGetter<ToOneChild> getIdGetter() {
        return __ID_GETTER;
    }

    @Override
    public CursorFactory<ToOneChild> getCursorFactory() {
        return __CURSOR_FACTORY;
    }

    @Internal
    static final class ToOneChildIdGetter implements IdGetter<ToOneChild> {
        @Override
        public long getId(ToOneChild object) {
            Long id = object.id;
            return id != null? id : 0;
        }
    }

    /** to-one */
    public static final RelationInfo<ToOneParent> parent =
            new RelationInfo<>(ToOneChild_.__INSTANCE, ToOneParent_.__INSTANCE, null, new ToOneGetter<ToOneChild>() {
                @Override
                public ToOne<ToOneParent> getToOne(ToOneChild entity) {
                    return entity.parent;
                }
            });

    /** to-one */
    public static final RelationInfo<ToOneParent> parentWithIdProperty =
            new RelationInfo<>(ToOneChild_.__INSTANCE, ToOneParent_.__INSTANCE, aParentId, new ToOneGetter<ToOneChild>() {
                @Override
                public ToOne<ToOneParent> getToOne(ToOneChild entity) {
                    return entity.parentWithIdProperty;
                }
            });

}
