package io.objectbox.processor.test;

import io.objectbox.BoxStore;
import io.objectbox.Cursor;
import io.objectbox.annotation.apihint.Internal;
import io.objectbox.internal.CursorFactory;
import io.objectbox.relation.ToMany;
import java.util.List;

//////
// NOTE: this is the EXPECTED generated source. During testing, only the syntax tree is compared, comments are ignored.
//////

/**
 * ObjectBox generated Cursor implementation for "BacklinkToOneTarget".
 * Note that this is a low-level class: usually you should stick to the Box class.
 */
public final class BacklinkToOneTargetCursor extends Cursor<BacklinkToOneTarget> {
    @Internal
    static final class Factory implements CursorFactory<BacklinkToOneTarget> {
        @Override
        public Cursor<BacklinkToOneTarget> createCursor(io.objectbox.Transaction tx, long cursorHandle, BoxStore boxStoreForEntities) {
            return new BacklinkToOneTargetCursor(tx, cursorHandle, boxStoreForEntities);
        }
    }

    private static final BacklinkToOneTarget_.BacklinkToOneTargetIdGetter ID_GETTER = BacklinkToOneTarget_.__ID_GETTER;



    public BacklinkToOneTargetCursor(io.objectbox.Transaction tx, long cursor, BoxStore boxStore) {
        super(tx, cursor, BacklinkToOneTarget_.__INSTANCE, boxStore);
    }

    @Override
    public long getId(BacklinkToOneTarget entity) {
        return ID_GETTER.getId(entity);
    }

    /**
     * Puts an object into its box.
     *
     * @return The ID of the object within its box.
     */
    @SuppressWarnings({"rawtypes", "unchecked"})
    @Override
    public long put(BacklinkToOneTarget entity) {
        Long id = entity.id;
        long __assignedId = collect004000(cursor, id != null ? id: 0, PUT_FLAG_FIRST | PUT_FLAG_COMPLETE,
                0, 0, 0, 0,
                0, 0, 0, 0);

        entity.id = __assignedId;

        entity.__boxStore = boxStoreForEntities;
        checkApplyToManyToDb(entity.sources, BacklinkToOneSource.class);
        return __assignedId;
    }

}
