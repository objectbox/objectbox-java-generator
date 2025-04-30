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
 * ObjectBox generated Cursor implementation for "BacklinkToOneListTarget".
 * Note that this is a low-level class: usually you should stick to the Box class.
 */
public final class BacklinkToOneListTargetCursor extends Cursor<BacklinkToOneListTarget> {
    @Internal
    static final class Factory implements CursorFactory<BacklinkToOneListTarget> {
        @Override
        public Cursor<BacklinkToOneListTarget> createCursor(io.objectbox.Transaction tx, long cursorHandle, BoxStore boxStoreForEntities) {
            return new BacklinkToOneListTargetCursor(tx, cursorHandle, boxStoreForEntities);
        }
    }

    private static final BacklinkToOneListTarget_.BacklinkToOneListTargetIdGetter ID_GETTER = BacklinkToOneListTarget_.__ID_GETTER;



    public BacklinkToOneListTargetCursor(io.objectbox.Transaction tx, long cursor, BoxStore boxStore) {
        super(tx, cursor, BacklinkToOneListTarget_.__INSTANCE, boxStore);
    }

    @Override
    public long getId(BacklinkToOneListTarget entity) {
        return ID_GETTER.getId(entity);
    }

    /**
     * Puts an object into its box.
     *
     * @return The ID of the object within its box.
     */
    @SuppressWarnings({"rawtypes", "unchecked"})
    @Override
    public long put(BacklinkToOneListTarget entity) {
        Long id = entity.id;
        long __assignedId = collect004000(cursor, id != null ? id: 0, PUT_FLAG_FIRST | PUT_FLAG_COMPLETE,
                0, 0, 0, 0,
                0, 0, 0, 0);

        entity.id = __assignedId;

        entity.__boxStore = boxStoreForEntities;
        checkApplyToManyToDb(entity.sources, BacklinkToOneListSource.class);
        return __assignedId;
    }

}
