package io.objectbox.processor.custom;

import io.objectbox.BoxStore;
import io.objectbox.BoxStoreBuilder;
import io.objectbox.ModelBuilder;
import io.objectbox.ModelBuilder.EntityBuilder;
import io.objectbox.model.PropertyFlags;
import io.objectbox.model.PropertyType;

import io.objectbox.processor.test.SimpleEntity;
import io.objectbox.processor.test.SimpleEntityCursor;
import io.objectbox.processor.test.SimpleEntity_;

//////
// NOTE: this is the EXPECTED generated source.
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
        modelBuilder.lastIndexId(1, 4551328960004588074L);
        modelBuilder.lastRelationId(0, 0L);

        EntityBuilder entityBuilder;

        entityBuilder = modelBuilder.entity("A");
        entityBuilder.id(1, 4858050548069557694L).lastPropertyId(24, 2870459311547136401L);
        entityBuilder.flags(io.objectbox.model.EntityFlags.USE_NO_ARG_CONSTRUCTOR);
        entityBuilder.property("id", PropertyType.Long).id(1, 8303367770402050741L)
                .flags(PropertyFlags.ID | PropertyFlags.ID_SELF_ASSIGNABLE | PropertyFlags.NON_PRIMITIVE_TYPE);
        entityBuilder.property("simpleShortPrimitive", PropertyType.Short).id(2, 2547454299149596320L)
                .flags(PropertyFlags.NOT_NULL);
        entityBuilder.property("simpleShort", PropertyType.Short).id(3, 1065398153566608274L)
                .flags(PropertyFlags.NON_PRIMITIVE_TYPE);
        entityBuilder.property("simpleIntPrimitive", PropertyType.Int).id(4, 7019841740086346212L)
                .flags(PropertyFlags.NOT_NULL);
        entityBuilder.property("simpleInt", PropertyType.Int).id(5, 2149606992334875025L)
                .flags(PropertyFlags.NON_PRIMITIVE_TYPE);
        entityBuilder.property("simpleLongPrimitive", PropertyType.Long).id(6, 3870774530785480176L)
                .flags(PropertyFlags.NOT_NULL);
        entityBuilder.property("simpleLong", PropertyType.Long).id(7, 6076934711032013633L)
                .flags(PropertyFlags.NON_PRIMITIVE_TYPE);
        entityBuilder.property("simpleFloatPrimitive", PropertyType.Float).id(8, 1005325329902505869L)
                .flags(PropertyFlags.NOT_NULL);
        entityBuilder.property("simpleFloat", PropertyType.Float).id(9, 6013849627054382655L)
                .flags(PropertyFlags.NON_PRIMITIVE_TYPE);
        entityBuilder.property("simpleDoublePrimitive", PropertyType.Double).id(10, 1714793024529017564L)
                .flags(PropertyFlags.NOT_NULL);
        entityBuilder.property("simpleDouble", PropertyType.Double).id(11, 1265968316355477242L)
                .flags(PropertyFlags.NON_PRIMITIVE_TYPE);
        entityBuilder.property("simpleBooleanPrimitive", PropertyType.Bool).id(12, 4472277657621006188L)
                .flags(PropertyFlags.NOT_NULL);
        entityBuilder.property("simpleBoolean", PropertyType.Bool).id(13, 7658102659822256293L)
                .flags(PropertyFlags.NON_PRIMITIVE_TYPE);
        entityBuilder.property("simpleBytePrimitive", PropertyType.Byte).id(14, 4560729911540009437L)
                .flags(PropertyFlags.NOT_NULL);
        entityBuilder.property("simpleByte", PropertyType.Byte).id(15, 1331388668561714029L)
                .flags(PropertyFlags.NON_PRIMITIVE_TYPE);
        entityBuilder.property("simpleDate", PropertyType.Date).id(16, 3711768428068804965L);
        entityBuilder.property("simpleCharPrimitive", PropertyType.Char).id(23, 1838261170942203646L)
                .flags(PropertyFlags.NOT_NULL);
        entityBuilder.property("simpleChar", PropertyType.Char).id(24, 2870459311547136401L)
                .flags(PropertyFlags.NON_PRIMITIVE_TYPE);
        entityBuilder.property("simpleString", PropertyType.String).id(17, 6798801512033870238L);
        entityBuilder.property("simpleByteArray", PropertyType.ByteVector).id(18, 5561205097618864485L);
        entityBuilder.property("indexedProperty", PropertyType.Int).id(19, 267919077724297667L)
                .flags(PropertyFlags.NON_PRIMITIVE_TYPE | PropertyFlags.INDEXED).indexId(1, 4551328960004588074L);
        entityBuilder.property("B", PropertyType.String).secondaryName("namedProperty").id(20, 8754346312277232208L);
        entityBuilder.property("customType", PropertyType.Int).id(21, 8133069888579241668L)
                .flags(PropertyFlags.NON_PRIMITIVE_TYPE);
        entityBuilder.property("customTypes", PropertyType.Int).id(22, 4772590935549770830L)
                .flags(PropertyFlags.NON_PRIMITIVE_TYPE);
        entityBuilder.entityDone();

        return modelBuilder.build();
    }

}
