package io.objectbox.processor.test;


import io.objectbox.BoxStore;
import io.objectbox.Cursor;
import io.objectbox.annotation.apihint.Internal;
import io.objectbox.internal.CursorFactory;
import io.objectbox.relation.ToOne;

//////
// NOTE: this is the EXPECTED generated source. During testing, only the syntax tree is compared, comments are ignored.
//////

/**
 * ObjectBox generated Cursor implementation for "RelationChild".
 * Note that this is a low-level class: usually you should stick to the Box class.
 */
public final class RelationChildCursor extends Cursor<RelationChild> {
    @Internal
    static final class Factory implements CursorFactory<RelationChild> {
        @Override
        public Cursor<RelationChild> createCursor(io.objectbox.Transaction tx, long cursorHandle, BoxStore boxStoreForEntities) {
            return new RelationChildCursor(tx, cursorHandle, boxStoreForEntities);
        }
    }

    private static final RelationChild_.RelationChildIdGetter ID_GETTER = RelationChild_.__ID_GETTER;


    private final static int __ID_parentId = RelationChild_.parentId.id;

    public RelationChildCursor(io.objectbox.Transaction tx, long cursor, BoxStore boxStore) {
        super(tx, cursor, RelationChild_.__INSTANCE, boxStore);
    }

    @Override
    public long getId(RelationChild entity) {
        return ID_GETTER.getId(entity);
    }

    /**
     * Puts an object into its box.
     *
     * @return The ID of the object within its box.
     */
    @SuppressWarnings({"rawtypes", "unchecked"})
    @Override
    public long put(RelationChild entity) {
        ToOne<RelationParent> parent = entity.parent;
        if(parent != null && parent.internalRequiresPutTarget()) {
            Cursor<RelationParent> targetCursor = getRelationTargetCursor(RelationParent.class);
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
                __ID_parentId, entity.parentId, 0, 0,
                0, 0, 0, 0,
                0, 0, 0, 0,
                0, 0, 0, 0);

        entity.id = __assignedId;

        entity.__boxStore = boxStoreForEntities;
        return __assignedId;
    }

}
