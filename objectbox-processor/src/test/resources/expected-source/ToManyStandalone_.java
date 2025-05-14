package io.objectbox.processor.test;

import io.objectbox.EntityInfo;
import io.objectbox.annotation.apihint.Internal;
import io.objectbox.internal.CursorFactory;
import io.objectbox.internal.IdGetter;
import io.objectbox.internal.ToManyGetter;
import io.objectbox.internal.ToOneGetter;
import io.objectbox.processor.test.ToManyStandaloneCursor.Factory;
import io.objectbox.relation.RelationInfo;
import io.objectbox.relation.ToOne;
import java.util.List;

//////
// NOTE: this is the EXPECTED generated source. During testing, only the syntax tree is compared, comments are ignored.
//////

/**
 * Properties for entity "ToManyStandalone". Can be used for QueryBuilder and for referencing DB names.
 */
public final class ToManyStandalone_ implements EntityInfo<ToManyStandalone> {

    // Leading underscores for static constants to avoid naming conflicts with property names

    public static final String __ENTITY_NAME = "ToManyStandalone";

    public static final int __ENTITY_ID = 1;

    public static final Class<ToManyStandalone> __ENTITY_CLASS = ToManyStandalone.class;

    public static final String __DB_NAME = "ToManyStandalone";

    public static final CursorFactory<ToManyStandalone> __CURSOR_FACTORY = new Factory();

    @Internal
    static final ToManyStandaloneIdGetter __ID_GETTER = new ToManyStandaloneIdGetter();

    public final static ToManyStandalone_ __INSTANCE = new ToManyStandalone_();

    public final static io.objectbox.Property<ToManyStandalone> id =
            new io.objectbox.Property<>(__INSTANCE, 0, 1, Long.class, "id", true, "id");

    @SuppressWarnings("unchecked")
    public final static io.objectbox.Property<ToManyStandalone>[] __ALL_PROPERTIES = new io.objectbox.Property[]{
            id
    };

    public final static io.objectbox.Property<ToManyStandalone> __ID_PROPERTY = id;

    @Override
    public String getEntityName() {
        return __ENTITY_NAME;
    }

    @Override
    public int getEntityId() {
        return __ENTITY_ID;
    }

    @Override
    public Class<ToManyStandalone> getEntityClass() {
        return __ENTITY_CLASS;
    }

    @Override
    public String getDbName() {
        return __DB_NAME;
    }

    @Override
    public io.objectbox.Property<ToManyStandalone>[] getAllProperties() {
        return __ALL_PROPERTIES;
    }

    @Override
    public io.objectbox.Property<ToManyStandalone> getIdProperty() {
        return __ID_PROPERTY;
    }

    @Override
    public IdGetter<ToManyStandalone> getIdGetter() {
        return __ID_GETTER;
    }

    @Override
    public CursorFactory<ToManyStandalone> getCursorFactory() {
        return __CURSOR_FACTORY;
    }

    @Internal
    static final class ToManyStandaloneIdGetter implements IdGetter<ToManyStandalone> {
        @Override
        public long getId(ToManyStandalone object) {
            Long id = object.id;
            return id != null? id : 0;
        }
    }

    /** To-many relation "children" to target entity "IdEntity". */
    public static final RelationInfo<ToManyStandalone, IdEntity> children = new RelationInfo<>(ToManyStandalone_.__INSTANCE, IdEntity_.__INSTANCE,
            new ToManyGetter<ToManyStandalone, IdEntity>() {
                @Override
                public List<IdEntity> getToMany(ToManyStandalone entity) {
                    return entity.children;
                }
            },
            1);

    /** To-many relation "childrenList" to target entity "IdEntity". */
    public static final RelationInfo<ToManyStandalone, IdEntity> childrenList = new RelationInfo<>(ToManyStandalone_.__INSTANCE, IdEntity_.__INSTANCE,
            new ToManyGetter<ToManyStandalone, IdEntity>() {
                @Override
                public List<IdEntity> getToMany(ToManyStandalone entity) {
                    return entity.childrenList;
                }
            },
            2);

}
