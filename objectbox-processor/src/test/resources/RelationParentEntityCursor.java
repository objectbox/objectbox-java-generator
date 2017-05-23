package io.objectbox.processor.test;

import io.objectbox.BoxStore;
import io.objectbox.Cursor;
import io.objectbox.Transaction;
import io.objectbox.annotation.apihint.Internal;
import io.objectbox.internal.CursorFactory;

//////
// NOTE: this is the EXPECTED generated source.
//////

/**
 * ObjectBox generated Cursor implementation for "RelationParentEntity".
 * Note that this is a low-level class: usually you should stick to the Box class.
 */
public final class RelationParentEntityCursor extends Cursor<RelationParentEntity> {
    @Internal
    static final class Factory implements CursorFactory<RelationParentEntity> {
        public Cursor<RelationParentEntity> createCursor(Transaction tx, long cursorHandle, BoxStore boxStoreForEntities) {
            return new RelationParentEntityCursor(tx, cursorHandle, boxStoreForEntities);
        }
    }

    private static final RelationParentEntity_.RelationParentEntityIdGetter ID_GETTER = RelationParentEntity_.__ID_GETTER;


    private final static int __ID_childId = RelationParentEntity_.childId.id;

    public RelationParentEntityCursor(Transaction tx, long cursor, BoxStore boxStore) {
        super(tx, cursor, RelationParentEntity_.__INSTANCE, boxStore);
    }

    @Override
    public final long getId(RelationParentEntity entity) {
        return ID_GETTER.getId(entity);
    }

    /**
     * Puts an object into its box.
     *
     * @return The ID of the object within its box.
     */
    @Override
    public final long put(RelationParentEntity entity) {
        if(entity.childToOne.internalRequiresPutTarget()) {
            Cursor<RelationChildEntity> targetCursor = getRelationTargetCursor(RelationChildEntity.class);
            try {
                entity.childToOne.internalPutTarget(targetCursor);
            } finally {
                targetCursor.close();
            }
        }
        long __assignedId = collect313311(cursor, entity.id, PUT_FLAG_FIRST | PUT_FLAG_COMPLETE,
                0, null, 0, null,
                0, null, 0, null,
                __ID_childId, entity.childId, 0, 0,
                0, 0, 0, 0,
                0, 0, 0, 0,
                0, 0, 0, 0);

        entity.id = __assignedId;
        entity.__boxStore = boxStoreForEntities;

        return __assignedId;
    }

    // TODO @Override
    //protected final void attachEntity(RelationParentEntity entity) {
    // TODO super.attachEntity(entity);
    //entity.__boxStore = boxStoreForEntities;
    //}


}
