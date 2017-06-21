package io.objectbox.processor.test;

import io.objectbox.BoxStore;
import io.objectbox.Cursor;
import io.objectbox.Transaction;
import io.objectbox.annotation.apihint.Internal;
import io.objectbox.internal.CursorFactory;
import io.objectbox.relation.ToOne;

//////
// NOTE: this is the EXPECTED generated source.
//////

/**
 * ObjectBox generated Cursor implementation for "ToOneChild".
 * Note that this is a low-level class: usually you should stick to the Box class.
 */
public final class ToOneChildCursor extends Cursor<ToOneChild> {
    @Internal
    static final class Factory implements CursorFactory<ToOneChild> {
        public Cursor<ToOneChild> createCursor(Transaction tx, long cursorHandle, BoxStore boxStoreForEntities) {
            return new ToOneChildCursor(tx, cursorHandle, boxStoreForEntities);
        }
    }

    private static final ToOneChild_.ToOneChildIdGetter ID_GETTER = ToOneChild_.__ID_GETTER;


    private final static int __ID_parentId = ToOneChild_.parentId.id;

    public ToOneChildCursor(Transaction tx, long cursor, BoxStore boxStore) {
        super(tx, cursor, ToOneChild_.__INSTANCE, boxStore);
    }

    @Override
    public final long getId(ToOneChild entity) {
        return ID_GETTER.getId(entity);
    }

    /**
     * Puts an object into its box.
     *
     * @return The ID of the object within its box.
     */
    @Override
    public final long put(ToOneChild entity) {
        ToOne<ToOneParent> parent = entity.parent;
        if(parent != null && parent.internalRequiresPutTarget()) {
            Cursor<ToOneParent> targetCursor = getRelationTargetCursor(ToOneParent.class);
            try {
                parent.internalPutTarget(targetCursor);
            } finally {
                targetCursor.close();
            }
        }
        Long id = entity.id;
        long __assignedId = collect313311(cursor, id != null ? id: 0, PUT_FLAG_FIRST | PUT_FLAG_COMPLETE,
                0, null, 0, null,
                0, null, 0, null,
                __ID_parentId, entity.parent.getTargetId(), 0, 0,
                0, 0, 0, 0,
                0, 0, 0, 0,
                0, 0, 0, 0);

        entity.id = __assignedId;

        entity.__boxStore = boxStoreForEntities;
        return __assignedId;
    }

}
