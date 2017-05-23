package io.objectbox.processor.test;

import io.objectbox.processor.test.ToOneParentEntityCursor.Factory;

import io.objectbox.EntityInfo;
import io.objectbox.Property;
import io.objectbox.annotation.apihint.Internal;
import io.objectbox.internal.CursorFactory;
import io.objectbox.internal.IdGetter;
import io.objectbox.relation.RelationInfo;

//////
// NOTE: this is the EXPECTED generated source.
//////

/**
 * Properties for entity "ToOneParentEntity". Can be used for QueryBuilder and for referencing DB names.
 */
public final class ToOneParentEntity_ implements EntityInfo<ToOneParentEntity> {

    // Leading underscores for static constants to avoid naming conflicts with property names

    public static final String __ENTITY_NAME = "ToOneParentEntity";

    public static final int __ENTITY_ID = 1;

    public static final Class<ToOneParentEntity> __ENTITY_CLASS = ToOneParentEntity.class;

    public static final String __DB_NAME = "ToOneParentEntity";

    public static final CursorFactory<ToOneParentEntity> __CURSOR_FACTORY = new Factory();

    @Internal
    static final ToOneParentEntityIdGetter __ID_GETTER = new ToOneParentEntityIdGetter();

    public final static Property id = new Property(0, 1, long.class, "id", true, "_id");
    public final static Property childId = new Property(1, 2, long.class, "childId");

    public final static Property[] __ALL_PROPERTIES = {
            id,
            childId
    };

    public final static Property __ID_PROPERTY = id;

    public final static ToOneParentEntity_ __INSTANCE = new ToOneParentEntity_();

    @Override
    public String getEntityName() {
        return __ENTITY_NAME;
    }

    @Override
    public int getEntityId() {
        return __ENTITY_ID;
    }

    @Override
    public Class<ToOneParentEntity> getEntityClass() {
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
    public IdGetter<ToOneParentEntity> getIdGetter() {
        return __ID_GETTER;
    }

    @Override
    public CursorFactory<ToOneParentEntity> getCursorFactory() {
        return __CURSOR_FACTORY;
    }

    @Internal
    static final class ToOneParentEntityIdGetter implements IdGetter<ToOneParentEntity> {
        public long getId(ToOneParentEntity object) {
            return object.id;
        }
    }

    static final RelationInfo<ToOneChildEntity> child =
            new RelationInfo<>(ToOneParentEntity_.__INSTANCE, ToOneChildEntity_.__INSTANCE, childId);

}
