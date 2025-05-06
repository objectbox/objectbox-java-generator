package io.objectbox.processor.custom;

import io.objectbox.BoxStore;
import io.objectbox.BoxStoreBuilder;
import io.objectbox.ModelBuilder;
import io.objectbox.ModelBuilder.EntityBuilder;
import io.objectbox.model.HnswDistanceType;
import io.objectbox.model.HnswFlags;
import io.objectbox.model.PropertyFlags;
import io.objectbox.model.PropertyType;
import io.objectbox.processor.test.IdEntity;
import io.objectbox.processor.test.IdEntityCursor;
import io.objectbox.processor.test.IdEntity_;
import io.objectbox.processor.test.SimpleEntity;
import io.objectbox.processor.test.SimpleEntityCursor;
import io.objectbox.processor.test.SimpleEntity_;

//////
// NOTE: this is the EXPECTED generated source. During testing, only the syntax tree is compared, comments are ignored.
//////

public class MyObjectBox {

    public static BoxStoreBuilder builder() {
        BoxStoreBuilder builder = new BoxStoreBuilder(getModel());
        builder.entity(SimpleEntity_.__INSTANCE);
        builder.entity(IdEntity_.__INSTANCE);
        return builder;
    }

    private static byte[] getModel() {
        ModelBuilder modelBuilder = new ModelBuilder();
        modelBuilder.lastEntityId(2, 7806468668391521694L);
        modelBuilder.lastIndexId(3, 4603777245389737989L);
        modelBuilder.lastRelationId(1, 1588763188636253926L);

        buildEntitySimpleEntity(modelBuilder);
        buildEntityIdEntity(modelBuilder);

        return modelBuilder.build();
    }

    private static void buildEntitySimpleEntity(ModelBuilder modelBuilder) {
        EntityBuilder entityBuilder = modelBuilder.entity("A");
        entityBuilder.id(1, 4858050548069557694L).lastPropertyId(40, 8362209306839260233L);
        entityBuilder.flags(io.objectbox.model.EntityFlags.USE_NO_ARG_CONSTRUCTOR);

        entityBuilder.property("id", PropertyType.Long).id(1, 8303367770402050741L)
                .flags(PropertyFlags.ID | PropertyFlags.ID_SELF_ASSIGNABLE);
        entityBuilder.property("simpleShortPrimitive", PropertyType.Short).id(2, 2547454299149596320L);
        entityBuilder.property("simpleShort", PropertyType.Short).id(3, 1065398153566608274L)
                .flags(PropertyFlags.NON_PRIMITIVE_TYPE);
        entityBuilder.property("simpleIntPrimitive", PropertyType.Int).id(4, 7019841740086346212L);
        entityBuilder.property("simpleInt", PropertyType.Int).id(5, 2149606992334875025L)
                .flags(PropertyFlags.NON_PRIMITIVE_TYPE);
        entityBuilder.property("simpleLongPrimitive", PropertyType.Long).id(6, 3870774530785480176L);
        entityBuilder.property("simpleLong", PropertyType.Long).id(7, 6076934711032013633L)
                .flags(PropertyFlags.NON_PRIMITIVE_TYPE);
        entityBuilder.property("simpleFloatPrimitive", PropertyType.Float).id(8, 1005325329902505869L);
        entityBuilder.property("simpleFloat", PropertyType.Float).id(9, 6013849627054382655L)
                .flags(PropertyFlags.NON_PRIMITIVE_TYPE);
        entityBuilder.property("simpleDoublePrimitive", PropertyType.Double).id(10, 1714793024529017564L);
        entityBuilder.property("simpleDouble", PropertyType.Double).id(11, 1265968316355477242L)
                .flags(PropertyFlags.NON_PRIMITIVE_TYPE);
        entityBuilder.property("simpleBooleanPrimitive", PropertyType.Bool).id(12, 4472277657621006188L);
        entityBuilder.property("simpleBoolean", PropertyType.Bool).id(13, 7658102659822256293L)
                .flags(PropertyFlags.NON_PRIMITIVE_TYPE);
        entityBuilder.property("simpleBytePrimitive", PropertyType.Byte).id(14, 4560729911540009437L);
        entityBuilder.property("simpleByte", PropertyType.Byte).id(15, 1331388668561714029L)
                .flags(PropertyFlags.NON_PRIMITIVE_TYPE);
        entityBuilder.property("simpleDate", PropertyType.Date).id(16, 3711768428068804965L);
        entityBuilder.property("simpleCharPrimitive", PropertyType.Char).id(23, 1838261170942203646L);
        entityBuilder.property("simpleChar", PropertyType.Char).id(24, 2870459311547136401L)
                .flags(PropertyFlags.NON_PRIMITIVE_TYPE);
        entityBuilder.property("simpleString", PropertyType.String).id(17, 6798801512033870238L);
        entityBuilder.property("simpleByteArray", PropertyType.ByteVector).id(18, 5561205097618864485L);
        entityBuilder.property("simpleStringArray", PropertyType.StringVector).id(26, 6896463341468697768L);
        entityBuilder.property("simpleStringList", PropertyType.StringVector).id(30, 8403031692314570141L)
                .flags(PropertyFlags.NON_PRIMITIVE_TYPE);
        entityBuilder.property("indexedProperty", PropertyType.Int).id(19, 267919077724297667L)
                .flags(PropertyFlags.NON_PRIMITIVE_TYPE | PropertyFlags.INDEXED).indexId(1, 4551328960004588074L);
        entityBuilder.property("B", PropertyType.String).secondaryName("namedProperty").id(20, 8754346312277232208L);
        entityBuilder.property("customType", PropertyType.Int).id(21, 8133069888579241668L)
                .flags(PropertyFlags.NON_PRIMITIVE_TYPE);
        entityBuilder.property("customTypes", PropertyType.Int).id(22, 4772590935549770830L)
                .flags(PropertyFlags.NON_PRIMITIVE_TYPE);
        entityBuilder.property("dateNanoPrimitive", PropertyType.DateNano).id(27, 1074761176027041968L);
        entityBuilder.property("dateNano", PropertyType.DateNano).id(28, 3312356255575143881L)
                .flags(PropertyFlags.NON_PRIMITIVE_TYPE);
        entityBuilder.property("idCompanion", PropertyType.Date).id(29, 5381545072285823697L)
                .flags(PropertyFlags.ID_COMPANION);
        entityBuilder.property("stringFlexMap", PropertyType.Flex).id(31, 553518482816470515L);
        entityBuilder.property("flexProperty", PropertyType.Flex).id(32, 4612611183471672203L);
        entityBuilder.property("booleanArray", PropertyType.BoolVector).id(40, 8362209306839260233L);
        entityBuilder.property("shortArray", PropertyType.ShortVector).id(33, 1725775029481425568L);
        entityBuilder.property("charArray", PropertyType.CharVector).id(34, 4774093004605044306L);
        entityBuilder.property("intArray", PropertyType.IntVector).id(35, 6776589142355880307L);
        entityBuilder.property("longArray", PropertyType.LongVector).id(36, 9006144140894927873L);
        entityBuilder.property("floatArray", PropertyType.FloatVector).id(37, 289592308738182865L);
        entityBuilder.property("doubleArray", PropertyType.DoubleVector).id(38, 6410324640289798251L);
        entityBuilder.property("floatArrayHnsw", PropertyType.FloatVector).id(39, 8513602819579966935L)
                .flags(PropertyFlags.INDEXED).indexId(3, 4603777245389737989L)
                .hnswParams(2, 30L, 100L, HnswFlags.DebugLogs | HnswFlags.DebugLogsDetailed | HnswFlags.VectorCacheSimdPaddingOff | HnswFlags.ReparationLimitCandidates, HnswDistanceType.Euclidean, 0.95F, 2097152L);
        entityBuilder.property("toOneId", "IdEntity", "toOne", PropertyType.Relation).id(25, 8807838229280449251L)
                .flags(PropertyFlags.VIRTUAL | PropertyFlags.INDEXED | PropertyFlags.INDEX_PARTIAL_SKIP_ZERO).indexId(2, 6174264050444102923L);

        entityBuilder.relation("toMany", 1, 1588763188636253926L, 2, 7806468668391521694L);

        entityBuilder.entityDone();
    }

    private static void buildEntityIdEntity(ModelBuilder modelBuilder) {
        EntityBuilder entityBuilder = modelBuilder.entity("IdEntity");
        entityBuilder.id(2, 7806468668391521694L).lastPropertyId(1, 4951803424764837731L);
        entityBuilder.flags(io.objectbox.model.EntityFlags.USE_NO_ARG_CONSTRUCTOR);

        entityBuilder.property("id", PropertyType.Long).id(1, 4951803424764837731L)
                .flags(PropertyFlags.ID | PropertyFlags.NON_PRIMITIVE_TYPE);


        entityBuilder.entityDone();
    }

}
