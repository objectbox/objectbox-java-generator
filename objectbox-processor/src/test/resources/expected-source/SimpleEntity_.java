package io.objectbox.processor.test;

import io.objectbox.EntityInfo;
import io.objectbox.annotation.apihint.Internal;
import io.objectbox.converter.FlexObjectConverter;
import io.objectbox.converter.StringFlexMapConverter;
import io.objectbox.internal.CursorFactory;
import io.objectbox.internal.IdGetter;
import io.objectbox.internal.ToManyGetter;
import io.objectbox.internal.ToOneGetter;
import io.objectbox.processor.test.SimpleEntity.SimpleEnum;
import io.objectbox.processor.test.SimpleEntity.SimpleEnumConverter;
import io.objectbox.processor.test.SimpleEntity.SimpleEnumListConverter;
import io.objectbox.processor.test.SimpleEntityCursor.Factory;
import io.objectbox.relation.RelationInfo;
import io.objectbox.relation.ToOne;
import java.lang.Object;
import java.util.List;
import java.util.Map;

//////
// NOTE: this is the EXPECTED generated source. During testing, only the syntax tree is compared, comments are ignored.
//////

/**
 * Properties for entity "A". Can be used for QueryBuilder and for referencing DB names.
 */
public final class SimpleEntity_ implements EntityInfo<SimpleEntity> {

    // Leading underscores for static constants to avoid naming conflicts with property names

    public static final String __ENTITY_NAME = "SimpleEntity";

    public static final int __ENTITY_ID = 1;

    public static final Class<SimpleEntity> __ENTITY_CLASS = SimpleEntity.class;

    public static final String __DB_NAME = "A";

    public static final CursorFactory<SimpleEntity> __CURSOR_FACTORY = new Factory();

    @Internal
    static final SimpleEntityIdGetter __ID_GETTER = new SimpleEntityIdGetter();

    public final static SimpleEntity_ __INSTANCE = new SimpleEntity_();

    public final static io.objectbox.Property<SimpleEntity> id =
            new io.objectbox.Property<>(__INSTANCE, 0, 1, long.class, "id", true, "id");

    public final static io.objectbox.Property<SimpleEntity> simpleShortPrimitive =
            new io.objectbox.Property<>(__INSTANCE, 1, 2, short.class, "simpleShortPrimitive");

    public final static io.objectbox.Property<SimpleEntity> simpleShort =
            new io.objectbox.Property<>(__INSTANCE, 2, 3, Short.class, "simpleShort");

    public final static io.objectbox.Property<SimpleEntity> simpleIntPrimitive =
            new io.objectbox.Property<>(__INSTANCE, 3, 4, int.class, "simpleIntPrimitive");

    public final static io.objectbox.Property<SimpleEntity> simpleInt =
            new io.objectbox.Property<>(__INSTANCE, 4, 5, Integer.class, "simpleInt");

    public final static io.objectbox.Property<SimpleEntity> simpleLongPrimitive =
            new io.objectbox.Property<>(__INSTANCE, 5, 6, long.class, "simpleLongPrimitive");

    public final static io.objectbox.Property<SimpleEntity> simpleLong =
            new io.objectbox.Property<>(__INSTANCE, 6, 7, Long.class, "simpleLong");

    public final static io.objectbox.Property<SimpleEntity> simpleFloatPrimitive =
            new io.objectbox.Property<>(__INSTANCE, 7, 8, float.class, "simpleFloatPrimitive");

    public final static io.objectbox.Property<SimpleEntity> simpleFloat =
            new io.objectbox.Property<>(__INSTANCE, 8, 9, Float.class, "simpleFloat");

    public final static io.objectbox.Property<SimpleEntity> simpleDoublePrimitive =
            new io.objectbox.Property<>(__INSTANCE, 9, 10, double.class, "simpleDoublePrimitive");

    public final static io.objectbox.Property<SimpleEntity> simpleDouble =
            new io.objectbox.Property<>(__INSTANCE, 10, 11, Double.class, "simpleDouble");

    public final static io.objectbox.Property<SimpleEntity> simpleBooleanPrimitive =
            new io.objectbox.Property<>(__INSTANCE, 11, 12, boolean.class, "simpleBooleanPrimitive");

    public final static io.objectbox.Property<SimpleEntity> simpleBoolean =
            new io.objectbox.Property<>(__INSTANCE, 12, 13, Boolean.class, "simpleBoolean");

    public final static io.objectbox.Property<SimpleEntity> simpleBytePrimitive =
            new io.objectbox.Property<>(__INSTANCE, 13, 14, byte.class, "simpleBytePrimitive");

    public final static io.objectbox.Property<SimpleEntity> simpleByte =
            new io.objectbox.Property<>(__INSTANCE, 14, 15, Byte.class, "simpleByte");

    public final static io.objectbox.Property<SimpleEntity> simpleDate =
            new io.objectbox.Property<>(__INSTANCE, 15, 16, java.util.Date.class, "simpleDate");

    public final static io.objectbox.Property<SimpleEntity> simpleCharPrimitive =
            new io.objectbox.Property<>(__INSTANCE, 16, 23, char.class, "simpleCharPrimitive");

    public final static io.objectbox.Property<SimpleEntity> simpleChar =
            new io.objectbox.Property<>(__INSTANCE, 17, 24, Character.class, "simpleChar");

    public final static io.objectbox.Property<SimpleEntity> simpleString =
            new io.objectbox.Property<>(__INSTANCE, 18, 17, String.class, "simpleString");

    public final static io.objectbox.Property<SimpleEntity> simpleByteArray =
            new io.objectbox.Property<>(__INSTANCE, 19, 18, byte[].class, "simpleByteArray");

    public final static io.objectbox.Property<SimpleEntity> simpleStringArray =
            new io.objectbox.Property<>(__INSTANCE, 20, 26, String[].class, "simpleStringArray");

    public final static io.objectbox.Property<SimpleEntity> simpleStringList =
            new io.objectbox.Property<>(__INSTANCE, 21, 30, java.util.List.class, "simpleStringList");

    public final static io.objectbox.Property<SimpleEntity> indexedProperty =
            new io.objectbox.Property<>(__INSTANCE, 22, 19, Integer.class, "indexedProperty");

    public final static io.objectbox.Property<SimpleEntity> namedProperty =
            new io.objectbox.Property<>(__INSTANCE, 23, 20, String.class, "namedProperty", false, "B");

    public final static io.objectbox.Property<SimpleEntity> customType =
            new io.objectbox.Property<>(__INSTANCE, 24, 21, int.class, "customType", false, "customType", SimpleEnumConverter.class, SimpleEnum.class);

    public final static io.objectbox.Property<SimpleEntity> customTypes =
            new io.objectbox.Property<>(__INSTANCE, 25, 22, int.class, "customTypes", false, "customTypes", SimpleEnumListConverter.class, List.class);

    public final static io.objectbox.Property<SimpleEntity> dateNanoPrimitive =
            new io.objectbox.Property<>(__INSTANCE, 26, 27, long.class, "dateNanoPrimitive");

    public final static io.objectbox.Property<SimpleEntity> dateNano =
            new io.objectbox.Property<>(__INSTANCE, 27, 28, Long.class, "dateNano");

    public final static io.objectbox.Property<SimpleEntity> idCompanion =
            new io.objectbox.Property<>(__INSTANCE, 28, 29, java.util.Date.class, "idCompanion");

    public final static io.objectbox.Property<SimpleEntity> stringFlexMap =
            new io.objectbox.Property<>(__INSTANCE, 29, 31, byte[].class, "stringFlexMap", false, "stringFlexMap", StringFlexMapConverter.class, Map.class);

    public final static io.objectbox.Property<SimpleEntity> flexProperty =
            new io.objectbox.Property<>(__INSTANCE, 30, 32, byte[].class, "flexProperty", false, "flexProperty", FlexObjectConverter.class, Object.class);

    public final static io.objectbox.Property<SimpleEntity> booleanArray =
            new io.objectbox.Property<>(__INSTANCE, 31, 40, boolean[].class, "booleanArray");

    public final static io.objectbox.Property<SimpleEntity> shortArray =
            new io.objectbox.Property<>(__INSTANCE, 32, 33, short[].class, "shortArray");

    public final static io.objectbox.Property<SimpleEntity> charArray =
            new io.objectbox.Property<>(__INSTANCE, 33, 34, char[].class, "charArray");

    public final static io.objectbox.Property<SimpleEntity> intArray =
            new io.objectbox.Property<>(__INSTANCE, 34, 35, int[].class, "intArray");

    public final static io.objectbox.Property<SimpleEntity> longArray =
            new io.objectbox.Property<>(__INSTANCE, 35, 36, long[].class, "longArray");

    public final static io.objectbox.Property<SimpleEntity> floatArray =
            new io.objectbox.Property<>(__INSTANCE, 36, 37, float[].class, "floatArray");

    public final static io.objectbox.Property<SimpleEntity> doubleArray =
            new io.objectbox.Property<>(__INSTANCE, 37, 38, double[].class, "doubleArray");

    public final static io.objectbox.Property<SimpleEntity> floatArrayHnsw =
            new io.objectbox.Property<>(__INSTANCE, 38, 39, float[].class, "floatArrayHnsw");

    public final static io.objectbox.Property<SimpleEntity> toOneId =
            new io.objectbox.Property<>(__INSTANCE, 39, 25, long.class, "toOneId", true);

    @SuppressWarnings("unchecked")
    public final static io.objectbox.Property<SimpleEntity>[] __ALL_PROPERTIES = new io.objectbox.Property[]{
            id,
            simpleShortPrimitive,
            simpleShort,
            simpleIntPrimitive,
            simpleInt,
            simpleLongPrimitive,
            simpleLong,
            simpleFloatPrimitive,
            simpleFloat,
            simpleDoublePrimitive,
            simpleDouble,
            simpleBooleanPrimitive,
            simpleBoolean,
            simpleBytePrimitive,
            simpleByte,
            simpleDate,
            simpleCharPrimitive,
            simpleChar,
            simpleString,
            simpleByteArray,
            simpleStringArray,
            simpleStringList,
            indexedProperty,
            namedProperty,
            customType,
            customTypes,
            dateNanoPrimitive,
            dateNano,
            idCompanion,
            stringFlexMap,
            flexProperty,
            booleanArray,
            shortArray,
            charArray,
            intArray,
            longArray,
            floatArray,
            doubleArray,
            floatArrayHnsw,
            toOneId
    };

    public final static io.objectbox.Property<SimpleEntity> __ID_PROPERTY = id;

    @Override
    public String getEntityName() {
        return __ENTITY_NAME;
    }

    @Override
    public int getEntityId() {
        return __ENTITY_ID;
    }

    @Override
    public Class<SimpleEntity> getEntityClass() {
        return __ENTITY_CLASS;
    }

    @Override
    public String getDbName() {
        return __DB_NAME;
    }

    @Override
    public io.objectbox.Property<SimpleEntity>[] getAllProperties() {
        return __ALL_PROPERTIES;
    }

    @Override
    public io.objectbox.Property<SimpleEntity> getIdProperty() {
        return __ID_PROPERTY;
    }

    @Override
    public IdGetter<SimpleEntity> getIdGetter() {
        return __ID_GETTER;
    }

    @Override
    public CursorFactory<SimpleEntity> getCursorFactory() {
        return __CURSOR_FACTORY;
    }

    @Internal
    static final class SimpleEntityIdGetter implements IdGetter<SimpleEntity> {
        @Override
        public long getId(SimpleEntity object) {
            return object.id;
        }
    }

    /** To-one relation "toOne" to target entity "IdEntity". */
    public static final RelationInfo<SimpleEntity, IdEntity> toOne =
            new RelationInfo<>(SimpleEntity_.__INSTANCE, IdEntity_.__INSTANCE, toOneId, new ToOneGetter<SimpleEntity, IdEntity>() {
                @Override
                public ToOne<IdEntity> getToOne(SimpleEntity entity) {
                    return entity.toOne;
                }
            });

    /** To-many relation "toMany" to target entity "IdEntity". */
    public static final RelationInfo<SimpleEntity, IdEntity> toMany = new RelationInfo<>(SimpleEntity_.__INSTANCE, IdEntity_.__INSTANCE,
            new ToManyGetter<SimpleEntity, IdEntity>() {
                @Override
                public List<IdEntity> getToMany(SimpleEntity entity) {
                    return entity.toMany;
                }
            },
            1);

}
