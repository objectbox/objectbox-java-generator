package io.objectbox.processor.test;

import io.objectbox.EntityInfo;
import io.objectbox.annotation.apihint.Internal;
import io.objectbox.internal.CursorFactory;
import io.objectbox.internal.IdGetter;
import io.objectbox.internal.ToManyGetter;
import io.objectbox.internal.ToOneGetter;
import io.objectbox.processor.test.BacklinkToOneTargetCursor.Factory;
import io.objectbox.relation.RelationInfo;
import io.objectbox.relation.ToOne;
import java.util.List;

//////
// NOTE: this is the EXPECTED generated source. During testing, only the syntax tree is compared, comments are ignored.
//////

/**
 * Properties for entity "BacklinkToOneTarget". Can be used for QueryBuilder and for referencing DB names.
 */
public final class BacklinkToOneTarget_ implements EntityInfo<BacklinkToOneTarget> {

    // Leading underscores for static constants to avoid naming conflicts with property names

    public static final String __ENTITY_NAME = "BacklinkToOneTarget";

    public static final int __ENTITY_ID = 1;

    public static final Class<BacklinkToOneTarget> __ENTITY_CLASS = BacklinkToOneTarget.class;

    public static final String __DB_NAME = "BacklinkToOneTarget";

    public static final CursorFactory<BacklinkToOneTarget> __CURSOR_FACTORY = new Factory();

    @Internal
    static final BacklinkToOneTargetIdGetter __ID_GETTER = new BacklinkToOneTargetIdGetter();

    public final static BacklinkToOneTarget_ __INSTANCE = new BacklinkToOneTarget_();

    public final static io.objectbox.Property<BacklinkToOneTarget> id =
            new io.objectbox.Property<>(__INSTANCE, 0, 1, Long.class, "id", true, "id");

    @SuppressWarnings("unchecked")
    public final static io.objectbox.Property<BacklinkToOneTarget>[] __ALL_PROPERTIES = new io.objectbox.Property[]{
            id
    };

    public final static io.objectbox.Property<BacklinkToOneTarget> __ID_PROPERTY = id;

    @Override
    public String getEntityName() {
        return __ENTITY_NAME;
    }

    @Override
    public int getEntityId() {
        return __ENTITY_ID;
    }

    @Override
    public Class<BacklinkToOneTarget> getEntityClass() {
        return __ENTITY_CLASS;
    }

    @Override
    public String getDbName() {
        return __DB_NAME;
    }

    @Override
    public io.objectbox.Property<BacklinkToOneTarget>[] getAllProperties() {
        return __ALL_PROPERTIES;
    }

    @Override
    public io.objectbox.Property<BacklinkToOneTarget> getIdProperty() {
        return __ID_PROPERTY;
    }

    @Override
    public IdGetter<BacklinkToOneTarget> getIdGetter() {
        return __ID_GETTER;
    }

    @Override
    public CursorFactory<BacklinkToOneTarget> getCursorFactory() {
        return __CURSOR_FACTORY;
    }

    @Internal
    static final class BacklinkToOneTargetIdGetter implements IdGetter<BacklinkToOneTarget> {
        @Override
        public long getId(BacklinkToOneTarget object) {
            Long id = object.id;
            return id != null? id : 0;
        }
    }

    /** To-many relation "sources" to target entity "BacklinkToOneSource". */
    public static final RelationInfo<BacklinkToOneTarget, BacklinkToOneSource> sources = new RelationInfo<>(BacklinkToOneTarget_.__INSTANCE, BacklinkToOneSource_.__INSTANCE,
            new ToManyGetter<BacklinkToOneTarget, BacklinkToOneSource>() {
                @Override
                public List<BacklinkToOneSource> getToMany(BacklinkToOneTarget entity) {
                    return entity.sources;
                }
            },
            BacklinkToOneSource_.targetId,
            new ToOneGetter<BacklinkToOneSource, BacklinkToOneTarget>() {
                @Override
                public ToOne<BacklinkToOneTarget> getToOne(BacklinkToOneSource entity) {
                    return entity.target;
                }
            });

}
