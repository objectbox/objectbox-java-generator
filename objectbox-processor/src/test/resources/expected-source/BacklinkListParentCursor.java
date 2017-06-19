package io.objectbox.processor.test;

import io.objectbox.BoxStore;
import io.objectbox.Cursor;
import io.objectbox.Transaction;
import io.objectbox.annotation.apihint.Internal;
import io.objectbox.internal.CursorFactory;
import io.objectbox.relation.ToMany;

//////
// NOTE: this is the EXPECTED generated source.
//////

/**
 * ObjectBox generated Cursor implementation for "BacklinkListParent".
 * Note that this is a low-level class: usually you should stick to the Box class.
 */
public final class BacklinkListParentCursor extends Cursor<BacklinkListParent> {
    @Internal
    static final class Factory implements CursorFactory<BacklinkListParent> {
        public Cursor<BacklinkListParent> createCursor(Transaction tx, long cursorHandle, BoxStore boxStoreForEntities) {
            return new BacklinkListParentCursor(tx, cursorHandle, boxStoreForEntities);
        }
    }

    private static final BacklinkListParent_.BacklinkListParentIdGetter ID_GETTER = BacklinkListParent_.__ID_GETTER;



    public BacklinkListParentCursor(Transaction tx, long cursor, BoxStore boxStore) {
        super(tx, cursor, BacklinkListParent_.__INSTANCE, boxStore);
    }

    @Override
    public final long getId(BacklinkListParent entity) {
        return ID_GETTER.getId(entity);
    }

    /**
     * Puts an object into its box.
     *
     * @return The ID of the object within its box.
     */
    @Override
    public final long put(BacklinkListParent entity) {
        Long id = entity.id;
        long __assignedId = collect004000(cursor, id != null ? id: 0, PUT_FLAG_FIRST | PUT_FLAG_COMPLETE,
                0, 0, 0, 0,
                0, 0, 0, 0);

        entity.id = __assignedId;
        entity.__boxStore = boxStoreForEntities;

        if (entity.children instanceof ToMany) {
            ToMany<BacklinkListChild> toMany = (ToMany<BacklinkListChild>) entity.children;
            if (toMany.internalRequiresPutTarget()) {
                toMany.internalPutTarget(getRelationTargetCursor(BacklinkListChild.class));
            }
        }
        return __assignedId;
    }

}
