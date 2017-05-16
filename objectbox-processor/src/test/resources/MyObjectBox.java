package io.objectbox.processor.test;

import io.objectbox.BoxStore;
import io.objectbox.BoxStoreBuilder;
import io.objectbox.ModelBuilder;
import io.objectbox.ModelBuilder.EntityBuilder;
import io.objectbox.model.PropertyFlags;
import io.objectbox.model.PropertyType;

//////
// NOTE: this is the EXPECTED MyObjectBox generated source.
//////
public class MyObjectBox {

    public static BoxStoreBuilder builder() {
        BoxStoreBuilder builder = new BoxStoreBuilder(getModel());
        builder.entity(SimpleEntity_.__INSTANCE);
        return builder;
    }

    private static byte[] getModel() {
        ModelBuilder modelBuilder = new ModelBuilder();
        modelBuilder.lastEntityId(1, 4858050548069557694L);
        modelBuilder.lastIndexId(0, 0L);

        EntityBuilder entityBuilder;

        entityBuilder = modelBuilder.entity("A");
        entityBuilder.id(1, 4858050548069557694L).lastPropertyId(21, 8133069888579241668L);
        entityBuilder.property("_id", PropertyType.Long)
                .flags(PropertyFlags.ID | PropertyFlags.ID_SELF_ASSIGNABLE | PropertyFlags.NOT_NULL);
        entityBuilder.property("simpleShortPrimitive", PropertyType.Short)
                .flags(PropertyFlags.NOT_NULL);
        entityBuilder.property("simpleShort", PropertyType.Short)
                .flags(PropertyFlags.NON_PRIMITIVE_TYPE);
        entityBuilder.property("simpleIntPrimitive", PropertyType.Int)
                .flags(PropertyFlags.NOT_NULL);
        entityBuilder.property("simpleInt", PropertyType.Int)
                .flags(PropertyFlags.NON_PRIMITIVE_TYPE);
        entityBuilder.property("simpleLongPrimitive", PropertyType.Long)
                .flags(PropertyFlags.NOT_NULL);
        entityBuilder.property("simpleLong", PropertyType.Long)
                .flags(PropertyFlags.NON_PRIMITIVE_TYPE);
        entityBuilder.property("simpleFloatPrimitive", PropertyType.Float)
                .flags(PropertyFlags.NOT_NULL);
        entityBuilder.property("simpleFloat", PropertyType.Float)
                .flags(PropertyFlags.NON_PRIMITIVE_TYPE);
        entityBuilder.property("simpleDoublePrimitive", PropertyType.Double)
                .flags(PropertyFlags.NOT_NULL);
        entityBuilder.property("simpleDouble", PropertyType.Double)
                .flags(PropertyFlags.NON_PRIMITIVE_TYPE);
        entityBuilder.property("simpleBooleanPrimitive", PropertyType.Bool)
                .flags(PropertyFlags.NOT_NULL);
        entityBuilder.property("simpleBoolean", PropertyType.Bool)
                .flags(PropertyFlags.NON_PRIMITIVE_TYPE);
        entityBuilder.property("simpleBytePrimitive", PropertyType.Byte)
                .flags(PropertyFlags.NOT_NULL);
        entityBuilder.property("simpleByte", PropertyType.Byte)
                .flags(PropertyFlags.NON_PRIMITIVE_TYPE);
        entityBuilder.property("simpleDate", PropertyType.Date);
        entityBuilder.property("simpleString", PropertyType.String);
        entityBuilder.property("simpleByteArray", PropertyType.ByteVector);
        entityBuilder.property("indexedProperty", PropertyType.Int)
                .flags(PropertyFlags.NON_PRIMITIVE_TYPE | PropertyFlags.INDEXED);
        entityBuilder.property("B", PropertyType.String);
        entityBuilder.property("customType", PropertyType.Int)
                .flags(PropertyFlags.NON_PRIMITIVE_TYPE);
        entityBuilder.entityDone();

        return modelBuilder.build();
    }

}
