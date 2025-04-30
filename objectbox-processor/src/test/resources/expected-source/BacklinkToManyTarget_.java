package io.objectbox.processor.test;

import io.objectbox.EntityInfo;
import io.objectbox.annotation.apihint.Internal;
import io.objectbox.internal.CursorFactory;
import io.objectbox.internal.IdGetter;
import io.objectbox.internal.ToManyGetter;
import io.objectbox.internal.ToOneGetter;
import io.objectbox.processor.test.BacklinkToManyTargetCursor.Factory;
import io.objectbox.relation.RelationInfo;
import io.objectbox.relation.ToOne;
import java.util.List;

//////
// NOTE: this is the EXPECTED generated source. During testing, only the syntax tree is compared, comments are ignored.
//////

/**
 * Properties for entity "BacklinkToManyTarget". Can be used for QueryBuilder and for referencing DB names.
 */
public final class BacklinkToManyTarget_ implements EntityInfo<BacklinkToManyTarget> {

    // Leading underscores for static constants to avoid naming conflicts with property names

    public static final String __ENTITY_NAME = "BacklinkToManyTarget";

    public static final int __ENTITY_ID = 2;

    public static final Class<BacklinkToManyTarget> __ENTITY_CLASS = BacklinkToManyTarget.class;

    public static final String __DB_NAME = "BacklinkToManyTarget";

    public static final CursorFactory<BacklinkToManyTarget> __CURSOR_FACTORY = new Factory();

    @Internal
    static final BacklinkToManyTargetIdGetter __ID_GETTER = new BacklinkToManyTargetIdGetter();

    public final static BacklinkToManyTarget_ __INSTANCE = new BacklinkToManyTarget_();

    public final static io.objectbox.Property<BacklinkToManyTarget> id =
            new io.objectbox.Property<>(__INSTANCE, 0, 1, Long.class, "id", true, "id");

    @SuppressWarnings("unchecked")
    public final static io.objectbox.Property<BacklinkToManyTarget>[] __ALL_PROPERTIES = new io.objectbox.Property[]{
            id
    };

    public final static io.objectbox.Property<BacklinkToManyTarget> __ID_PROPERTY = id;

    @Override
    public String getEntityName() {
        return __ENTITY_NAME;
    }

    @Override
    public int getEntityId() {
        return __ENTITY_ID;
    }

    @Override
    public Class<BacklinkToManyTarget> getEntityClass() {
        return __ENTITY_CLASS;
    }

    @Override
    public String getDbName() {
        return __DB_NAME;
    }

    @Override
    public io.objectbox.Property<BacklinkToManyTarget>[] getAllProperties() {
        return __ALL_PROPERTIES;
    }

    @Override
    public io.objectbox.Property<BacklinkToManyTarget> getIdProperty() {
        return __ID_PROPERTY;
    }

    @Override
    public IdGetter<BacklinkToManyTarget> getIdGetter() {
        return __ID_GETTER;
    }

    @Override
    public CursorFactory<BacklinkToManyTarget> getCursorFactory() {
        return __CURSOR_FACTORY;
    }

    @Internal
    static final class BacklinkToManyTargetIdGetter implements IdGetter<BacklinkToManyTarget> {
        @Override
        public long getId(BacklinkToManyTarget object) {
            Long id = object.id;
            return id != null? id : 0;
        }
    }

    /** To-many relation "sources" to target entity "BacklinkToManySource". */
    public static final RelationInfo<BacklinkToManyTarget, BacklinkToManySource> sources = new RelationInfo<>(BacklinkToManyTarget_.__INSTANCE, BacklinkToManySource_.__INSTANCE,
            new ToManyGetter<BacklinkToManyTarget, BacklinkToManySource>() {
                @Override
                public List<BacklinkToManySource> getToMany(BacklinkToManyTarget entity) {
                    return entity.sources;
                }
            },
            new ToManyGetter<BacklinkToManySource, BacklinkToManyTarget>() {
                @Override
                public List<BacklinkToManyTarget> getToMany(BacklinkToManySource entity) {
                    return entity.targets;
                }
            }, 1);

}
