package io.objectbox.processor.test;

import io.objectbox.EntityInfo;
import io.objectbox.annotation.apihint.Internal;
import io.objectbox.internal.CursorFactory;
import io.objectbox.internal.IdGetter;
import io.objectbox.internal.ToManyGetter;
import io.objectbox.internal.ToOneGetter;
import io.objectbox.processor.test.BacklinkToOneListTargetCursor.Factory;
import io.objectbox.relation.RelationInfo;
import io.objectbox.relation.ToOne;
import java.util.List;

//////
// NOTE: this is the EXPECTED generated source. During testing, only the syntax tree is compared, comments are ignored.
//////

/**
 * Properties for entity "BacklinkToOneListTarget". Can be used for QueryBuilder and for referencing DB names.
 */
public final class BacklinkToOneListTarget_ implements EntityInfo<BacklinkToOneListTarget> {

    // Leading underscores for static constants to avoid naming conflicts with property names

    public static final String __ENTITY_NAME = "BacklinkToOneListTarget";

    public static final int __ENTITY_ID = 1;

    public static final Class<BacklinkToOneListTarget> __ENTITY_CLASS = BacklinkToOneListTarget.class;

    public static final String __DB_NAME = "BacklinkToOneListTarget";

    public static final CursorFactory<BacklinkToOneListTarget> __CURSOR_FACTORY = new Factory();

    @Internal
    static final BacklinkToOneListTargetIdGetter __ID_GETTER = new BacklinkToOneListTargetIdGetter();

    public final static BacklinkToOneListTarget_ __INSTANCE = new BacklinkToOneListTarget_();

    public final static io.objectbox.Property<BacklinkToOneListTarget> id =
            new io.objectbox.Property<>(__INSTANCE, 0, 1, Long.class, "id", true, "id");

    @SuppressWarnings("unchecked")
    public final static io.objectbox.Property<BacklinkToOneListTarget>[] __ALL_PROPERTIES = new io.objectbox.Property[]{
            id
    };

    public final static io.objectbox.Property<BacklinkToOneListTarget> __ID_PROPERTY = id;

    @Override
    public String getEntityName() {
        return __ENTITY_NAME;
    }

    @Override
    public int getEntityId() {
        return __ENTITY_ID;
    }

    @Override
    public Class<BacklinkToOneListTarget> getEntityClass() {
        return __ENTITY_CLASS;
    }

    @Override
    public String getDbName() {
        return __DB_NAME;
    }

    @Override
    public io.objectbox.Property<BacklinkToOneListTarget>[] getAllProperties() {
        return __ALL_PROPERTIES;
    }

    @Override
    public io.objectbox.Property<BacklinkToOneListTarget> getIdProperty() {
        return __ID_PROPERTY;
    }

    @Override
    public IdGetter<BacklinkToOneListTarget> getIdGetter() {
        return __ID_GETTER;
    }

    @Override
    public CursorFactory<BacklinkToOneListTarget> getCursorFactory() {
        return __CURSOR_FACTORY;
    }

    @Internal
    static final class BacklinkToOneListTargetIdGetter implements IdGetter<BacklinkToOneListTarget> {
        @Override
        public long getId(BacklinkToOneListTarget object) {
            Long id = object.id;
            return id != null? id : 0;
        }
    }

    /** To-many relation "sources" to target entity "BacklinkToOneListSource". */
    public static final RelationInfo<BacklinkToOneListTarget, BacklinkToOneListSource> sources = new RelationInfo<>(BacklinkToOneListTarget_.__INSTANCE, BacklinkToOneListSource_.__INSTANCE,
            new ToManyGetter<BacklinkToOneListTarget, BacklinkToOneListSource>() {
                @Override
                public List<BacklinkToOneListSource> getToMany(BacklinkToOneListTarget entity) {
                    return entity.sources;
                }
            },
            BacklinkToOneListSource_.targetId,
            new ToOneGetter<BacklinkToOneListSource, BacklinkToOneListTarget>() {
                @Override
                public ToOne<BacklinkToOneListTarget> getToOne(BacklinkToOneListSource entity) {
                    return entity.target;
                }
            });

}
