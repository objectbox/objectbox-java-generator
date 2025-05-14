package io.objectbox.processor.test;

import io.objectbox.BoxStore;
import io.objectbox.BoxStoreBuilder;
import io.objectbox.ModelBuilder;
import io.objectbox.ModelBuilder.EntityBuilder;
import io.objectbox.model.PropertyFlags;
import io.objectbox.model.PropertyType;

//////
// NOTE: this is the EXPECTED generated source. During testing, only the syntax tree is compared, comments are ignored.
//////

public class MyObjectBox {

    public static BoxStoreBuilder builder() {
        BoxStoreBuilder builder = new BoxStoreBuilder(getModel());
        builder.entity(IndexGenerated_.__INSTANCE);
        return builder;
    }

    private static byte[] getModel() {
        ModelBuilder modelBuilder = new ModelBuilder();
        modelBuilder.lastEntityId(1, 8909216917646328500L);
        modelBuilder.lastIndexId(4, 6816316644989602931L);
        modelBuilder.lastRelationId(0, 0L);

        buildEntityIndexGenerated(modelBuilder);

        return modelBuilder.build();
    }

    private static void buildEntityIndexGenerated(ModelBuilder modelBuilder) {
        EntityBuilder entityBuilder = modelBuilder.entity("IndexGenerated");
        entityBuilder.id(1, 8909216917646328500L).lastPropertyId(5, 386689427203097845L);
        entityBuilder.flags(io.objectbox.model.EntityFlags.USE_NO_ARG_CONSTRUCTOR);

        entityBuilder.property("id", PropertyType.Long).id(1, 5446539300635330200L)
                .flags(PropertyFlags.ID);
        entityBuilder.property("defaultProp", PropertyType.Int).id(2, 5084698942674518401L)
                .flags(PropertyFlags.INDEXED).indexId(1, 6484382999829587572L);
        entityBuilder.property("valueProp", PropertyType.Bool).id(3, 7492569418772139189L)
                .flags(PropertyFlags.INDEXED).indexId(2, 6991639645044933453L);
        entityBuilder.property("hashProp", PropertyType.String).id(4, 104351152796407233L)
                .flags(PropertyFlags.INDEX_HASH).indexId(3, 1690874977862683848L);
        entityBuilder.property("hash64Prop", PropertyType.String).id(5, 386689427203097845L)
                .flags(PropertyFlags.INDEX_HASH64).indexId(4, 6816316644989602931L);

        entityBuilder.entityDone();
    }

}