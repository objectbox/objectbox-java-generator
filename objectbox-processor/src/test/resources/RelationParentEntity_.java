package io.objectbox.processor.test;

import io.objectbox.processor.test.RelationParentEntityCursor.Factory;

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
 * Properties for entity "RelationParentEntity". Can be used for QueryBuilder and for referencing DB names.
 */
public final class RelationParentEntity_ implements EntityInfo<RelationParentEntity> {

    // Leading underscores for static constants to avoid naming conflicts with property names

    public static final String __ENTITY_NAME = "RelationParentEntity";

    public static final int __ENTITY_ID = 1;

    public static final Class<RelationParentEntity> __ENTITY_CLASS = RelationParentEntity.class;

    public static final String __DB_NAME = "RelationParentEntity";

    public static final CursorFactory<RelationParentEntity> __CURSOR_FACTORY = new Factory();

    @Internal
    static final RelationParentEntityIdGetter __ID_GETTER = new RelationParentEntityIdGetter();

    public final static Property id = new Property(0, 1, long.class, "id", true, "_id");
    public final static Property childId = new Property(1, 2, long.class, "childId");

    public final static Property[] __ALL_PROPERTIES = {
            id,
            childId
    };

    public final static Property __ID_PROPERTY = id;

    public final static RelationParentEntity_ __INSTANCE = new RelationParentEntity_();

    @Override
    public String getEntityName() {
        return __ENTITY_NAME;
    }

    @Override
    public int getEntityId() {
        return __ENTITY_ID;
    }

    @Override
    public Class<RelationParentEntity> getEntityClass() {
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
    public IdGetter<RelationParentEntity> getIdGetter() {
        return __ID_GETTER;
    }

    @Override
    public CursorFactory<RelationParentEntity> getCursorFactory() {
        return __CURSOR_FACTORY;
    }

    @Internal
    static final class RelationParentEntityIdGetter implements IdGetter<RelationParentEntity> {
        public long getId(RelationParentEntity object) {
            return object.id;
        }
    }

    static final RelationInfo<RelationChildEntity> child =
            new RelationInfo<>(RelationParentEntity_.__INSTANCE, RelationChildEntity_.__INSTANCE, childId);

}
