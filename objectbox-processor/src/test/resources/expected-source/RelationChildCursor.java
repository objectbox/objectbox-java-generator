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
 * ObjectBox generated Cursor implementation for "RelationChild".
 * Note that this is a low-level class: usually you should stick to the Box class.
 */
public final class RelationChildCursor extends Cursor<RelationChild> {
    @Internal
    static final class Factory implements CursorFactory<RelationChild> {
        public Cursor<RelationChild> createCursor(Transaction tx, long cursorHandle, BoxStore boxStoreForEntities) {
            return new RelationChildCursor(tx, cursorHandle, boxStoreForEntities);
        }
    }

    private static final RelationChild_.RelationChildIdGetter ID_GETTER = RelationChild_.__ID_GETTER;


    private final static int __ID_parentId = RelationChild_.parentId.id;

    public RelationChildCursor(Transaction tx, long cursor, BoxStore boxStore) {
        super(tx, cursor, RelationChild_.__INSTANCE, boxStore);
    }

    @Override
    public final long getId(RelationChild entity) {
        return ID_GETTER.getId(entity);
    }

    /**
     * Puts an object into its box.
     *
     * @return The ID of the object within its box.
     */
    @Override
    public final long put(RelationChild entity) {
        if(entity.parentToOne.internalRequiresPutTarget()) {
            Cursor<RelationParent> targetCursor = getRelationTargetCursor(RelationParent.class);
            try {
                entity.parentToOne.internalPutTarget(targetCursor);
            } finally {
                targetCursor.close();
            }
        }
        long __assignedId = collect313311(cursor, entity.id, PUT_FLAG_FIRST | PUT_FLAG_COMPLETE,
                0, null, 0, null,
                0, null, 0, null,
                __ID_parentId, entity.parentId, 0, 0,
                0, 0, 0, 0,
                0, 0, 0, 0,
                0, 0, 0, 0);

        entity.id = __assignedId;
        entity.__boxStore = boxStoreForEntities;

        return __assignedId;
    }

    // TODO @Override
    //protected final void attachEntity(RelationChild entity) {
    // TODO super.attachEntity(entity);
    //entity.__boxStore = boxStoreForEntities;
    //}


}