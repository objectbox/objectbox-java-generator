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
 * ObjectBox generated Cursor implementation for "ToOneParentEntity".
 * Note that this is a low-level class: usually you should stick to the Box class.
 */
public final class ToOneParentEntityCursor extends Cursor<ToOneParentEntity> {
    @Internal
    static final class Factory implements CursorFactory<ToOneParentEntity> {
        public Cursor<ToOneParentEntity> createCursor(Transaction tx, long cursorHandle, BoxStore boxStoreForEntities) {
            return new ToOneParentEntityCursor(tx, cursorHandle, boxStoreForEntities);
        }
    }

    private static final ToOneParentEntity_.ToOneParentEntityIdGetter ID_GETTER = ToOneParentEntity_.__ID_GETTER;


    private final static int __ID_childId = ToOneParentEntity_.childId.id;

    public ToOneParentEntityCursor(Transaction tx, long cursor, BoxStore boxStore) {
        super(tx, cursor, ToOneParentEntity_.__INSTANCE, boxStore);
    }

    @Override
    public final long getId(ToOneParentEntity entity) {
        return ID_GETTER.getId(entity);
    }

    /**
     * Puts an object into its box.
     *
     * @return The ID of the object within its box.
     */
    @Override
    public final long put(ToOneParentEntity entity) {
        if(entity.child.internalRequiresPutTarget()) {
            Cursor<ToOneChildEntity> targetCursor = getRelationTargetCursor(ToOneChildEntity.class);
            try {
                entity.child.internalPutTarget(targetCursor);
            } finally {
                targetCursor.close();
            }
        }
        long __assignedId = collect313311(cursor, entity.id, PUT_FLAG_FIRST | PUT_FLAG_COMPLETE,
                0, null, 0, null,
                0, null, 0, null,
                __ID_childId, entity.child.getTargetId(), 0, 0,
                0, 0, 0, 0,
                0, 0, 0, 0,
                0, 0, 0, 0);

        entity.id = __assignedId;
        entity.__boxStore = boxStoreForEntities;

        return __assignedId;
    }

    // TODO @Override
    //protected final void attachEntity(ToOneParentEntity entity) {
    // TODO super.attachEntity(entity);
    //entity.__boxStore = boxStoreForEntities;
    //}


}
