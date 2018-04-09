package io.objectbox.processor.test;

import io.objectbox.processor.test.BacklinkToOneListTargetCursor.Factory;

import io.objectbox.EntityInfo;
import io.objectbox.annotation.apihint.Internal;
import io.objectbox.internal.CursorFactory;
import io.objectbox.internal.IdGetter;
import io.objectbox.relation.RelationInfo;
import io.objectbox.relation.ToOne;
import io.objectbox.internal.ToOneGetter;
import io.objectbox.internal.ToManyGetter;

import java.util.List;

//////
// NOTE: this is the EXPECTED generated source.
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

    public final static io.objectbox.Property id = new io.objectbox.Property(0, 1, Long.class, "id", true, "id");

    public final static io.objectbox.Property[] __ALL_PROPERTIES = {
            id
    };

    public final static io.objectbox.Property __ID_PROPERTY = id;

    public final static BacklinkToOneListTarget_ __INSTANCE = new BacklinkToOneListTarget_();

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
    public io.objectbox.Property[] getAllProperties() {
        return __ALL_PROPERTIES;
    }

    @Override
    public io.objectbox.Property getIdProperty() {
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

    /** to-many */
    public static final RelationInfo<BacklinkToOneListSource> sources = new RelationInfo<>(BacklinkToOneListTarget_.__INSTANCE, BacklinkToOneListSource_.__INSTANCE,
            new ToManyGetter<BacklinkToOneListTarget>() {
                @Override
                public List<BacklinkToOneListSource> getToMany(BacklinkToOneListTarget entity) {
                    return entity.sources;
                }
            },
            BacklinkToOneListSource_.targetId,
            new ToOneGetter<BacklinkToOneListSource>() {
                @Override
                public ToOne<BacklinkToOneListTarget> getToOne(BacklinkToOneListSource entity) {
                    return entity.target;
                }
            });

}
