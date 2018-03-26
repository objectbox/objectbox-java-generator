package io.objectbox.processor.test;

import io.objectbox.processor.test.SimpleEntityCursor.Factory;

import io.objectbox.EntityInfo;
import io.objectbox.annotation.apihint.Internal;
import io.objectbox.internal.CursorFactory;
import io.objectbox.internal.IdGetter;


import io.objectbox.processor.test.SimpleEntity.SimpleEnum;
import io.objectbox.processor.test.SimpleEntity.SimpleEnumConverter;
import io.objectbox.processor.test.SimpleEntity.SimpleEnumListConverter;
import java.util.List;

//////
// NOTE: this is the EXPECTED generated source.
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

    public final static io.objectbox.Property id = new io.objectbox.Property(0, 1, Long.class, "id", true, "id");
    public final static io.objectbox.Property simpleShortPrimitive = new io.objectbox.Property(1, 2, short.class, "simpleShortPrimitive");
    public final static io.objectbox.Property simpleShort = new io.objectbox.Property(2, 3, Short.class, "simpleShort");
    public final static io.objectbox.Property simpleIntPrimitive = new io.objectbox.Property(3, 4, int.class, "simpleIntPrimitive");
    public final static io.objectbox.Property simpleInt = new io.objectbox.Property(4, 5, Integer.class, "simpleInt");
    public final static io.objectbox.Property simpleLongPrimitive = new io.objectbox.Property(5, 6, long.class, "simpleLongPrimitive");
    public final static io.objectbox.Property simpleLong = new io.objectbox.Property(6, 7, Long.class, "simpleLong");
    public final static io.objectbox.Property simpleFloatPrimitive = new io.objectbox.Property(7, 8, float.class, "simpleFloatPrimitive");
    public final static io.objectbox.Property simpleFloat = new io.objectbox.Property(8, 9, Float.class, "simpleFloat");
    public final static io.objectbox.Property simpleDoublePrimitive = new io.objectbox.Property(9, 10, double.class, "simpleDoublePrimitive");
    public final static io.objectbox.Property simpleDouble = new io.objectbox.Property(10, 11, Double.class, "simpleDouble");
    public final static io.objectbox.Property simpleBooleanPrimitive = new io.objectbox.Property(11, 12, boolean.class, "simpleBooleanPrimitive");
    public final static io.objectbox.Property simpleBoolean = new io.objectbox.Property(12, 13, Boolean.class, "simpleBoolean");
    public final static io.objectbox.Property simpleBytePrimitive = new io.objectbox.Property(13, 14, byte.class, "simpleBytePrimitive");
    public final static io.objectbox.Property simpleByte = new io.objectbox.Property(14, 15, Byte.class, "simpleByte");
    public final static io.objectbox.Property simpleDate = new io.objectbox.Property(15, 16, java.util.Date.class, "simpleDate");
    public final static io.objectbox.Property simpleCharPrimitive = new io.objectbox.Property(16, 23, char.class, "simpleCharPrimitive");
    public final static io.objectbox.Property simpleChar = new io.objectbox.Property(17, 24, Character.class, "simpleChar");
    public final static io.objectbox.Property simpleString = new io.objectbox.Property(18, 17, String.class, "simpleString");
    public final static io.objectbox.Property simpleByteArray = new io.objectbox.Property(19, 18, byte[].class, "simpleByteArray");
    public final static io.objectbox.Property indexedProperty = new io.objectbox.Property(20, 19, Integer.class, "indexedProperty");
    public final static io.objectbox.Property namedProperty = new io.objectbox.Property(21, 20, String.class, "namedProperty", false, "B");
    public final static io.objectbox.Property customType = new io.objectbox.Property(22, 21, int.class, "customType", false, "customType", SimpleEnumConverter.class, SimpleEnum.class);
    public final static io.objectbox.Property customTypes = new io.objectbox.Property(23, 22, int.class, "customTypes", false, "customTypes", SimpleEnumListConverter.class, List.class);

    public final static io.objectbox.Property[] __ALL_PROPERTIES = {
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
            indexedProperty,
            namedProperty,
            customType,
            customTypes
    };

    public final static io.objectbox.Property __ID_PROPERTY = id;

    public final static SimpleEntity_ __INSTANCE = new SimpleEntity_();

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
    public io.objectbox.Property[] getAllProperties() {
        return __ALL_PROPERTIES;
    }

    @Override
    public io.objectbox.Property getIdProperty() {
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
            Long id = object.id;
            return id != null? id : 0;
        }
    }

}
