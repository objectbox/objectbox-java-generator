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
 * ObjectBox generated Cursor implementation for "ToManyStandalone".
 * Note that this is a low-level class: usually you should stick to the Box class.
 */
public final class ToManyStandaloneCursor extends Cursor<ToManyStandalone> {
    @Internal
    static final class Factory implements CursorFactory<ToManyStandalone> {
        @Override
        public Cursor<ToManyStandalone> createCursor(io.objectbox.Transaction tx, long cursorHandle, BoxStore boxStoreForEntities) {
            return new ToManyStandaloneCursor(tx, cursorHandle, boxStoreForEntities);
        }
    }

    private static final ToManyStandalone_.ToManyStandaloneIdGetter ID_GETTER = ToManyStandalone_.__ID_GETTER;



    public ToManyStandaloneCursor(io.objectbox.Transaction tx, long cursor, BoxStore boxStore) {
        super(tx, cursor, ToManyStandalone_.__INSTANCE, boxStore);
    }

    @Override
    public long getId(ToManyStandalone entity) {
        return ID_GETTER.getId(entity);
    }

    /**
     * Puts an object into its box.
     *
     * @return The ID of the object within its box.
     */
    @SuppressWarnings({"rawtypes", "unchecked"})
    @Override
    public long put(ToManyStandalone entity) {
        Long id = entity.id;
        long __assignedId = collect004000(cursor, id != null ? id: 0, PUT_FLAG_FIRST | PUT_FLAG_COMPLETE,
                0, 0, 0, 0,
                0, 0, 0, 0);

        entity.id = __assignedId;

        attachEntity(entity);
        checkApplyToManyToDb(entity.children, IdEntity.class);
        checkApplyToManyToDb(entity.childrenList, IdEntity.class);
        return __assignedId;
    }

    private void attachEntity(ToManyStandalone entity) {
        // Transformer will create __boxStore field in entity and init it here:
        // entity.__boxStore = boxStoreForEntities;
    }

}
