package io.objectbox.processor.test;

import io.objectbox.BoxStore;
import io.objectbox.Cursor;
import io.objectbox.annotation.apihint.Internal;
import io.objectbox.converter.FlexObjectConverter;
import io.objectbox.converter.StringFlexMapConverter;
import io.objectbox.internal.CursorFactory;
import io.objectbox.processor.test.SimpleEntity.SimpleEnum;
import io.objectbox.processor.test.SimpleEntity.SimpleEnumConverter;
import io.objectbox.processor.test.SimpleEntity.SimpleEnumListConverter;
import io.objectbox.relation.ToMany;
import io.objectbox.relation.ToOne;
import java.lang.Object;
import java.util.List;
import java.util.Map;

//////
// NOTE: this is the EXPECTED generated source. During testing, only the syntax tree is compared, comments are ignored.
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
    private final StringFlexMapConverter stringFlexMapConverter = new StringFlexMapConverter();
    private final FlexObjectConverter flexPropertyConverter = new FlexObjectConverter();

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
    private final static int __ID_simpleStringArray = SimpleEntity_.simpleStringArray.id;
    private final static int __ID_simpleStringList = SimpleEntity_.simpleStringList.id;
    private final static int __ID_indexedProperty = SimpleEntity_.indexedProperty.id;
    private final static int __ID_namedProperty = SimpleEntity_.namedProperty.id;
    private final static int __ID_customType = SimpleEntity_.customType.id;
    private final static int __ID_customTypes = SimpleEntity_.customTypes.id;
    private final static int __ID_dateNanoPrimitive = SimpleEntity_.dateNanoPrimitive.id;
    private final static int __ID_dateNano = SimpleEntity_.dateNano.id;
    private final static int __ID_idCompanion = SimpleEntity_.idCompanion.id;
    private final static int __ID_stringFlexMap = SimpleEntity_.stringFlexMap.id;
    private final static int __ID_flexProperty = SimpleEntity_.flexProperty.id;
    private final static int __ID_booleanArray = SimpleEntity_.booleanArray.id;
    private final static int __ID_shortArray = SimpleEntity_.shortArray.id;
    private final static int __ID_charArray = SimpleEntity_.charArray.id;
    private final static int __ID_intArray = SimpleEntity_.intArray.id;
    private final static int __ID_longArray = SimpleEntity_.longArray.id;
    private final static int __ID_floatArray = SimpleEntity_.floatArray.id;
    private final static int __ID_doubleArray = SimpleEntity_.doubleArray.id;
    private final static int __ID_floatArrayHnsw = SimpleEntity_.floatArrayHnsw.id;
    private final static int __ID_toOneId = SimpleEntity_.toOneId.id;

    public SimpleEntityCursor(io.objectbox.Transaction tx, long cursor, BoxStore boxStore) {
        super(tx, cursor, SimpleEntity_.__INSTANCE, boxStore);
    }

    @Override
    public long getId(SimpleEntity entity) {
        return ID_GETTER.getId(entity);
    }

    /**
     * Puts an object into its box.
     *
     * @return The ID of the object within its box.
     */
    @SuppressWarnings({"rawtypes", "unchecked"})
    @Override
    public long put(SimpleEntity entity) {
        ToOne<IdEntity> toOne = entity.toOne;
        if(toOne != null && toOne.internalRequiresPutTarget()) {
            Cursor<IdEntity> targetCursor = getRelationTargetCursor(IdEntity.class);
            try {
                toOne.internalPutTarget(targetCursor);
            } finally {
                targetCursor.close();
            }
        }
        boolean[] booleanArray = entity.booleanArray;
        int __id31 = booleanArray != null ? __ID_booleanArray : 0;

        collectBooleanArray(cursor, 0, PUT_FLAG_FIRST,
                __id31, booleanArray);

        short[] shortArray = entity.shortArray;
        int __id32 = shortArray != null ? __ID_shortArray : 0;

        collectShortArray(cursor, 0, 0,
                __id32, shortArray);

        char[] charArray = entity.charArray;
        int __id33 = charArray != null ? __ID_charArray : 0;

        collectCharArray(cursor, 0, 0,
                __id33, charArray);

        int[] intArray = entity.intArray;
        int __id34 = intArray != null ? __ID_intArray : 0;

        collectIntArray(cursor, 0, 0,
                __id34, intArray);

        long[] longArray = entity.longArray;
        int __id35 = longArray != null ? __ID_longArray : 0;

        collectLongArray(cursor, 0, 0,
                __id35, longArray);

        float[] floatArray = entity.floatArray;
        int __id36 = floatArray != null ? __ID_floatArray : 0;

        collectFloatArray(cursor, 0, 0,
                __id36, floatArray);

        float[] floatArrayHnsw = entity.floatArrayHnsw;
        int __id38 = floatArrayHnsw != null ? __ID_floatArrayHnsw : 0;

        collectFloatArray(cursor, 0, 0,
                __id38, floatArrayHnsw);

        double[] doubleArray = entity.doubleArray;
        int __id37 = doubleArray != null ? __ID_doubleArray : 0;

        collectDoubleArray(cursor, 0, 0,
                __id37, doubleArray);

        String[] simpleStringArray = entity.simpleStringArray;
        int __id20 = simpleStringArray != null ? __ID_simpleStringArray : 0;

        collectStringArray(cursor, 0, 0,
                __id20, simpleStringArray);

        java.util.List<String> simpleStringList = entity.simpleStringList;
        int __id21 = simpleStringList != null ? __ID_simpleStringList : 0;

        collectStringList(cursor, 0, 0,
                __id21, simpleStringList);

        String simpleString = entity.simpleString;
        int __id18 = simpleString != null ? __ID_simpleString : 0;
        String namedProperty = entity.namedProperty;
        int __id23 = namedProperty != null ? __ID_namedProperty : 0;
        byte[] simpleByteArray = entity.simpleByteArray;
        int __id19 = simpleByteArray != null ? __ID_simpleByteArray : 0;
        Map stringFlexMap = entity.stringFlexMap;
        int __id29 = stringFlexMap != null ? __ID_stringFlexMap : 0;
        Object flexProperty = entity.flexProperty;
        int __id30 = flexProperty != null ? __ID_flexProperty : 0;

        collect430000(cursor, 0, 0,
                __id18, simpleString, __id23, namedProperty,
                0, null, 0, null,
                __id19, simpleByteArray, __id29, __id29 != 0 ? stringFlexMapConverter.convertToDatabaseValue(stringFlexMap) : null,
                __id30, __id30 != 0 ? flexPropertyConverter.convertToDatabaseValue(flexProperty) : null);

        Long simpleLong = entity.simpleLong;
        int __id6 = simpleLong != null ? __ID_simpleLong : 0;
        Integer simpleInt = entity.simpleInt;
        int __id4 = simpleInt != null ? __ID_simpleInt : 0;
        Integer indexedProperty = entity.indexedProperty;
        int __id22 = indexedProperty != null ? __ID_indexedProperty : 0;

        collect313311(cursor, 0, 0,
                0, null, 0, null,
                0, null, 0, null,
                __ID_simpleLongPrimitive, entity.simpleLongPrimitive, __id6, __id6 != 0 ? simpleLong : 0,
                __ID_toOneId, entity.toOne.getTargetId(), __ID_simpleIntPrimitive, entity.simpleIntPrimitive,
                __id4, __id4 != 0 ? simpleInt : 0, __id22, __id22 != 0 ? indexedProperty : 0,
                __ID_simpleFloatPrimitive, entity.simpleFloatPrimitive, __ID_simpleDoublePrimitive, entity.simpleDoublePrimitive);

        Long dateNano = entity.dateNano;
        int __id27 = dateNano != null ? __ID_dateNano : 0;
        java.util.Date simpleDate = entity.simpleDate;
        int __id15 = simpleDate != null ? __ID_simpleDate : 0;
        SimpleEnum customType = entity.customType;
        int __id24 = customType != null ? __ID_customType : 0;
        List customTypes = entity.customTypes;
        int __id25 = customTypes != null ? __ID_customTypes : 0;
        Float simpleFloat = entity.simpleFloat;
        int __id8 = simpleFloat != null ? __ID_simpleFloat : 0;
        Double simpleDouble = entity.simpleDouble;
        int __id10 = simpleDouble != null ? __ID_simpleDouble : 0;

        collect313311(cursor, 0, 0,
                0, null, 0, null,
                0, null, 0, null,
                __ID_dateNanoPrimitive, entity.dateNanoPrimitive, __id27, __id27 != 0 ? dateNano : 0,
                __id15, __id15 != 0 ? simpleDate.getTime() : 0, __id24, __id24 != 0 ? customTypeConverter.convertToDatabaseValue(customType) : 0,
                __id25, __id25 != 0 ? customTypesConverter.convertToDatabaseValue(customTypes) : 0, __ID_simpleShortPrimitive, entity.simpleShortPrimitive,
                __id8, __id8 != 0 ? simpleFloat : 0, __id10, __id10 != 0 ? simpleDouble : 0);

        java.util.Date idCompanion = entity.idCompanion;
        int __id28 = idCompanion != null ? __ID_idCompanion : 0;
        Short simpleShort = entity.simpleShort;
        int __id2 = simpleShort != null ? __ID_simpleShort : 0;
        Character simpleChar = entity.simpleChar;
        int __id17 = simpleChar != null ? __ID_simpleChar : 0;

        collect004000(cursor, 0, 0,
                __id28, __id28 != 0 ? idCompanion.getTime() : 0, __id2, __id2 != 0 ? simpleShort : 0,
                __ID_simpleCharPrimitive, entity.simpleCharPrimitive, __id17, __id17 != 0 ? simpleChar : 0);

        Byte simpleByte = entity.simpleByte;
        int __id14 = simpleByte != null ? __ID_simpleByte : 0;
        Boolean simpleBoolean = entity.getSimpleBoolean();
        int __id12 = simpleBoolean != null ? __ID_simpleBoolean : 0;

        long __assignedId = collect004000(cursor, entity.id, PUT_FLAG_COMPLETE,
                __ID_simpleBytePrimitive, entity.simpleBytePrimitive, __id14, __id14 != 0 ? simpleByte : 0,
                __ID_simpleBooleanPrimitive, entity.isSimpleBooleanPrimitive() ? 1 : 0, __id12, __id12 != 0 ? simpleBoolean ? 1 : 0 : 0);

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
