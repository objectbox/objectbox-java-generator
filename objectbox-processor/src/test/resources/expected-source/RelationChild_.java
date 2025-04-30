package io.objectbox.processor.test;

import io.objectbox.EntityInfo;
import io.objectbox.annotation.apihint.Internal;
import io.objectbox.internal.CursorFactory;
import io.objectbox.internal.IdGetter;
import io.objectbox.internal.ToOneGetter;
import io.objectbox.processor.test.RelationChildCursor.Factory;
import io.objectbox.relation.RelationInfo;
import io.objectbox.relation.ToOne;

//////
// NOTE: this is the EXPECTED generated source. During testing, only the syntax tree is compared, comments are ignored.
//////

/**
 * Properties for entity "RelationChild". Can be used for QueryBuilder and for referencing DB names.
 */
public final class RelationChild_ implements EntityInfo<RelationChild> {

    // Leading underscores for static constants to avoid naming conflicts with property names

    public static final String __ENTITY_NAME = "RelationChild";

    public static final int __ENTITY_ID = 1;

    public static final Class<RelationChild> __ENTITY_CLASS = RelationChild.class;

    public static final String __DB_NAME = "RelationChild";

    public static final CursorFactory<RelationChild> __CURSOR_FACTORY = new Factory();

    @Internal
    static final RelationChildIdGetter __ID_GETTER = new RelationChildIdGetter();

    public final static RelationChild_ __INSTANCE = new RelationChild_();

    public final static io.objectbox.Property<RelationChild> id =
            new io.objectbox.Property<>(__INSTANCE, 0, 1, Long.class, "id", true, "id");

    public final static io.objectbox.Property<RelationChild> parentId =
            new io.objectbox.Property<>(__INSTANCE, 1, 2, long.class, "parentId");

    @SuppressWarnings("unchecked")
    public final static io.objectbox.Property<RelationChild>[] __ALL_PROPERTIES = new io.objectbox.Property[]{
            id,
            parentId
    };

    public final static io.objectbox.Property<RelationChild> __ID_PROPERTY = id;

    @Override
    public String getEntityName() {
        return __ENTITY_NAME;
    }

    @Override
    public int getEntityId() {
        return __ENTITY_ID;
    }

    @Override
    public Class<RelationChild> getEntityClass() {
        return __ENTITY_CLASS;
    }

    @Override
    public String getDbName() {
        return __DB_NAME;
    }

    @Override
    public io.objectbox.Property<RelationChild>[] getAllProperties() {
        return __ALL_PROPERTIES;
    }

    @Override
    public io.objectbox.Property<RelationChild> getIdProperty() {
        return __ID_PROPERTY;
    }

    @Override
    public IdGetter<RelationChild> getIdGetter() {
        return __ID_GETTER;
    }

    @Override
    public CursorFactory<RelationChild> getCursorFactory() {
        return __CURSOR_FACTORY;
    }

    @Internal
    static final class RelationChildIdGetter implements IdGetter<RelationChild> {
        @Override
        public long getId(RelationChild object) {
            Long id = object.id;
            return id != null? id : 0;
        }
    }

    /** To-one relation "parent" to target entity "RelationParent". */
    public static final RelationInfo<RelationChild, RelationParent> parent =
            new RelationInfo<>(RelationChild_.__INSTANCE, RelationParent_.__INSTANCE, parentId, new ToOneGetter<RelationChild, RelationParent>() {
                @Override
                public ToOne<RelationParent> getToOne(RelationChild entity) {
                    return entity.parent;
                }
            });

}
