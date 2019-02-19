package io.objectbox.processor.test;

import java.util.List;

import io.objectbox.BoxStore;
import io.objectbox.Cursor;
import io.objectbox.annotation.apihint.Internal;
import io.objectbox.internal.CursorFactory;
import io.objectbox.relation.ToMany;
import io.objectbox.relation.ToOne;

import io.objectbox.processor.test.SimpleEntity.SimpleEnum;
import io.objectbox.processor.test.SimpleEntity.SimpleEnumConverter;
import io.objectbox.processor.test.SimpleEntity.SimpleEnumListConverter;
import java.util.List;

//////
// NOTE: this is the EXPECTED generated source.
//////

/**
 * ObjectBox generated Cursor implementation for "A".
 * Note that this is a low-level class: usually you should stick to the Box class.
 */
public final class SimpleEntityCursor extends Cursor<SimpleEntity> {
    @Internal
    static final class Factory implements CursorFactory<SimpleEntity> {
        @Override
        public Cursor<SimpleEntity> createCursor(io.objectbox.Transaction tx, long cursorHandle, BoxStore boxStoreForEntities) {
            return new SimpleEntityCursor(tx, cursorHandle, boxStoreForEntities);
        }
    }

    private static final SimpleEntity_.SimpleEntityIdGetter ID_GETTER = SimpleEntity_.__ID_GETTER;

    private final SimpleEnumConverter customTypeConverter = new SimpleEnumConverter();
    private final SimpleEnumListConverter customTypesConverter = new SimpleEnumListConverter();

    private final static int __ID_simpleShortPrimitive = SimpleEntity_.simpleShortPrimitive.id;
    private final static int __ID_simpleShort = SimpleEntity_.simpleShort.id;
    private final static int __ID_simpleIntPrimitive = SimpleEntity_.simpleIntPrimitive.id;
    private final static int __ID_simpleInt = SimpleEntity_.simpleInt.id;
    private final static int __ID_simpleLongPrimitive = SimpleEntity_.simpleLongPrimitive.id;
    private final static int __ID_simpleLong = SimpleEntity_.simpleLong.id;
    private final static int __ID_simpleFloatPrimitive = SimpleEntity_.simpleFloatPrimitive.id;
    private final static int __ID_simpleFloat = SimpleEntity_.simpleFloat.id;
    private final static int __ID_simpleDoublePrimitive = SimpleEntity_.simpleDoublePrimitive.id;
    private final static int __ID_simpleDouble = SimpleEntity_.simpleDouble.id;
    private final static int __ID_simpleBooleanPrimitive = SimpleEntity_.simpleBooleanPrimitive.id;
    private final static int __ID_simpleBoolean = SimpleEntity_.simpleBoolean.id;
    private final static int __ID_simpleBytePrimitive = SimpleEntity_.simpleBytePrimitive.id;
    private final static int __ID_simpleByte = SimpleEntity_.simpleByte.id;
    private final static int __ID_simpleDate = SimpleEntity_.simpleDate.id;
    private final static int __ID_simpleCharPrimitive = SimpleEntity_.simpleCharPrimitive.id;
    private final static int __ID_simpleChar = SimpleEntity_.simpleChar.id;
    private final static int __ID_simpleString = SimpleEntity_.simpleString.id;
    private final static int __ID_simpleByteArray = SimpleEntity_.simpleByteArray.id;
    private final static int __ID_indexedProperty = SimpleEntity_.indexedProperty.id;
    private final static int __ID_namedProperty = SimpleEntity_.namedProperty.id;
    private final static int __ID_customType = SimpleEntity_.customType.id;
    private final static int __ID_customTypes = SimpleEntity_.customTypes.id;
    private final static int __ID_toOneId = SimpleEntity_.toOneId.id;

    public SimpleEntityCursor(io.objectbox.Transaction tx, long cursor, BoxStore boxStore) {
        super(tx, cursor, SimpleEntity_.__INSTANCE, boxStore);
    }

    @Override
    public final long getId(SimpleEntity entity) {
        return ID_GETTER.getId(entity);
    }

    /**
     * Puts an object into its box.
     *
     * @return The ID of the object within its box.
     */
    @Override
    public final long put(SimpleEntity entity) {
        ToOne<IdEntity> toOne = entity.toOne;
        if(toOne != null && toOne.internalRequiresPutTarget()) {
            Cursor<IdEntity> targetCursor = getRelationTargetCursor(IdEntity.class);
            try {
                toOne.internalPutTarget(targetCursor);
            } finally {
                targetCursor.close();
            }
        }
        String simpleString = entity.simpleString;
        int __id18 = simpleString != null ? __ID_simpleString : 0;
        String namedProperty = entity.namedProperty;
        int __id21 = namedProperty != null ? __ID_namedProperty : 0;
        byte[] simpleByteArray = entity.simpleByteArray;
        int __id19 = simpleByteArray != null ? __ID_simpleByteArray : 0;
        Long simpleLong = entity.simpleLong;
        int __id6 = simpleLong != null ? __ID_simpleLong : 0;
        Integer simpleInt = entity.simpleInt;
        int __id4 = simpleInt != null ? __ID_simpleInt : 0;
        Integer indexedProperty = entity.indexedProperty;
        int __id20 = indexedProperty != null ? __ID_indexedProperty : 0;

        collect313311(cursor, 0, PUT_FLAG_FIRST,
                __id18, simpleString, __id21, namedProperty,
                0, null, __id19, simpleByteArray,
                __ID_simpleLongPrimitive, entity.simpleLongPrimitive, __id6, __id6 != 0 ? simpleLong : 0,
                __ID_toOneId, entity.toOne.getTargetId(), __ID_simpleIntPrimitive, entity.simpleIntPrimitive,
                __id4, __id4 != 0 ? simpleInt : 0, __id20, __id20 != 0 ? indexedProperty : 0,
                __ID_simpleFloatPrimitive, entity.simpleFloatPrimitive, __ID_simpleDoublePrimitive, entity.simpleDoublePrimitive);

        java.util.Date simpleDate = entity.simpleDate;
        int __id15 = simpleDate != null ? __ID_simpleDate : 0;
        SimpleEnum customType = entity.customType;
        int __id22 = customType != null ? __ID_customType : 0;
        List customTypes = entity.customTypes;
        int __id23 = customTypes != null ? __ID_customTypes : 0;
        Short simpleShort = entity.simpleShort;
        int __id2 = simpleShort != null ? __ID_simpleShort : 0;
        Float simpleFloat = entity.simpleFloat;
        int __id8 = simpleFloat != null ? __ID_simpleFloat : 0;
        Double simpleDouble = entity.simpleDouble;
        int __id10 = simpleDouble != null ? __ID_simpleDouble : 0;

        collect313311(cursor, 0, 0,
                0, null, 0, null,
                0, null, 0, null,
                __id15, __id15 != 0 ? simpleDate.getTime() : 0, __id22, __id22 != 0 ? customTypeConverter.convertToDatabaseValue(customType) : 0,
                __id23, __id23 != 0 ? customTypesConverter.convertToDatabaseValue(customTypes) : 0, __ID_simpleShortPrimitive, entity.simpleShortPrimitive,
                __id2, __id2 != 0 ? simpleShort : 0, __ID_simpleCharPrimitive, entity.simpleCharPrimitive,
                __id8, __id8 != 0 ? simpleFloat : 0, __id10, __id10 != 0 ? simpleDouble : 0);

        Long id = entity.id;
        Character simpleChar = entity.simpleChar;
        int __id17 = simpleChar != null ? __ID_simpleChar : 0;
        Byte simpleByte = entity.simpleByte;
        int __id14 = simpleByte != null ? __ID_simpleByte : 0;
        Boolean simpleBoolean = entity.getSimpleBoolean();
        int __id12 = simpleBoolean != null ? __ID_simpleBoolean : 0;

        long __assignedId = collect313311(cursor, id != null ? id: 0, PUT_FLAG_COMPLETE,
                0, null, 0, null,
                0, null, 0, null,
                __id17, __id17 != 0 ? simpleChar : 0, __ID_simpleBytePrimitive, entity.simpleBytePrimitive,
                __id14, __id14 != 0 ? simpleByte : 0, __ID_simpleBooleanPrimitive, entity.isSimpleBooleanPrimitive() ? 1 : 0,
                __id12, __id12 != 0 ? simpleBoolean ? 1 : 0 : 0, 0, 0,
                0, 0, 0, 0);

        entity.id = __assignedId;

        attachEntity(entity);
        checkApplyToManyToDb(entity.toMany, IdEntity.class);
        return __assignedId;
    }

    private void attachEntity(SimpleEntity entity) {
        // Transformer will create __boxStore field in entity and init it here:
        // entity.__boxStore = boxStoreForEntities;
    }

}
