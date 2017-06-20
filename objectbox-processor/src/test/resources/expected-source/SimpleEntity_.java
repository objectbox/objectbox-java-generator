package io.objectbox.processor.test;

import io.objectbox.processor.test.SimpleEntityCursor.Factory;

import io.objectbox.EntityInfo;
import io.objectbox.Property;
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

    public final static Property id = new Property(0, 1, Long.class, "id", true, "id");
    public final static Property simpleShortPrimitive = new Property(1, 2, short.class, "simpleShortPrimitive");
    public final static Property simpleShort = new Property(2, 3, Short.class, "simpleShort");
    public final static Property simpleIntPrimitive = new Property(3, 4, int.class, "simpleIntPrimitive");
    public final static Property simpleInt = new Property(4, 5, Integer.class, "simpleInt");
    public final static Property simpleLongPrimitive = new Property(5, 6, long.class, "simpleLongPrimitive");
    public final static Property simpleLong = new Property(6, 7, Long.class, "simpleLong");
    public final static Property simpleFloatPrimitive = new Property(7, 8, float.class, "simpleFloatPrimitive");
    public final static Property simpleFloat = new Property(8, 9, Float.class, "simpleFloat");
    public final static Property simpleDoublePrimitive = new Property(9, 10, double.class, "simpleDoublePrimitive");
    public final static Property simpleDouble = new Property(10, 11, Double.class, "simpleDouble");
    public final static Property simpleBooleanPrimitive = new Property(11, 12, boolean.class, "simpleBooleanPrimitive");
    public final static Property simpleBoolean = new Property(12, 13, Boolean.class, "simpleBoolean");
    public final static Property simpleBytePrimitive = new Property(13, 14, byte.class, "simpleBytePrimitive");
    public final static Property simpleByte = new Property(14, 15, Byte.class, "simpleByte");
    public final static Property simpleDate = new Property(15, 16, java.util.Date.class, "simpleDate");
    public final static Property simpleString = new Property(16, 17, String.class, "simpleString");
    public final static Property simpleByteArray = new Property(17, 18, byte[].class, "simpleByteArray");
    public final static Property indexedProperty = new Property(18, 19, Integer.class, "indexedProperty");
    public final static Property namedProperty = new Property(19, 20, String.class, "namedProperty", false, "B");
    public final static Property customType = new Property(20, 21, int.class, "customType", false, "customType", SimpleEnumConverter.class, SimpleEnum.class);
    public final static Property customTypes = new Property(21, 22, int.class, "customTypes", false, "customTypes", SimpleEnumListConverter.class, List.class);

    public final static Property[] __ALL_PROPERTIES = {
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
            simpleString,
            simpleByteArray,
            indexedProperty,
            namedProperty,
            customType,
            customTypes
    };

    public final static Property __ID_PROPERTY = id;

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
    public Property[] getAllProperties() {
        return __ALL_PROPERTIES;
    }

    @Override
    public Property getIdProperty() {
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
        public long getId(SimpleEntity object) {
            Long id = object.id;
            return id != null? id : 0;
        }
    }

}
