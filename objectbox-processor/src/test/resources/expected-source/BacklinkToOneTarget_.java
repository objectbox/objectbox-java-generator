package io.objectbox.processor.test;

import io.objectbox.processor.test.BacklinkToOneTargetCursor.Factory;

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

    public final static io.objectbox.Property id = new io.objectbox.Property(0, 1, Long.class, "id", true, "id");

    public final static io.objectbox.Property[] __ALL_PROPERTIES = {
            id
    };

    public final static io.objectbox.Property __ID_PROPERTY = id;

    public final static BacklinkToOneTarget_ __INSTANCE = new BacklinkToOneTarget_();

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
    public io.objectbox.Property[] getAllProperties() {
        return __ALL_PROPERTIES;
    }

    @Override
    public io.objectbox.Property getIdProperty() {
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

    /** to-many */
    public static final RelationInfo<BacklinkToOneSource> sources = new RelationInfo<>(BacklinkToOneTarget_.__INSTANCE, BacklinkToOneSource_.__INSTANCE,
            new ToManyGetter<BacklinkToOneTarget>() {
                @Override
                public List<BacklinkToOneSource> getToMany(BacklinkToOneTarget entity) {
                    return entity.sources;
                }
            },
            BacklinkToOneSource_.targetId,
            new ToOneGetter<BacklinkToOneSource>() {
                @Override
                public ToOne<BacklinkToOneTarget> getToOne(BacklinkToOneSource entity) {
                    return entity.target;
                }
            });

}