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
 * ObjectBox generated Cursor implementation for "BacklinkToManyParent".
 * Note that this is a low-level class: usually you should stick to the Box class.
 */
public final class BacklinkToManyParentCursor extends Cursor<BacklinkToManyParent> {
    @Internal
    static final class Factory implements CursorFactory<BacklinkToManyParent> {
        public Cursor<BacklinkToManyParent> createCursor(Transaction tx, long cursorHandle, BoxStore boxStoreForEntities) {
            return new BacklinkToManyParentCursor(tx, cursorHandle, boxStoreForEntities);
        }
    }

    private static final BacklinkToManyParent_.BacklinkToManyParentIdGetter ID_GETTER = BacklinkToManyParent_.__ID_GETTER;



    public BacklinkToManyParentCursor(Transaction tx, long cursor, BoxStore boxStore) {
        super(tx, cursor, BacklinkToManyParent_.__INSTANCE, boxStore);
    }

    @Override
    public final long getId(BacklinkToManyParent entity) {
        return ID_GETTER.getId(entity);
    }

    /**
     * Puts an object into its box.
     *
     * @return The ID of the object within its box.
     */
    @Override
    public final long put(BacklinkToManyParent entity) {
        Long id = entity.id;
        long __assignedId = collect004000(cursor, id != null ? id: 0, PUT_FLAG_FIRST | PUT_FLAG_COMPLETE,
                0, 0, 0, 0,
                0, 0, 0, 0);

        entity.id = __assignedId;
        entity.__boxStore = boxStoreForEntities;

        if (entity.children instanceof ToMany) {
            ToMany<BacklinkToManyChild> toMany = (ToMany<BacklinkToManyChild>) entity.children;
            if (toMany.internalRequiresPutTarget()) {
                toMany.internalPutTarget(getRelationTargetCursor(BacklinkToManyChild.class));
            }
        }
        return __assignedId;
    }

}
