package io.objectbox.processor.test;

import io.objectbox.BoxStore;
import io.objectbox.BoxStoreBuilder;
import io.objectbox.ModelBuilder;
import io.objectbox.ModelBuilder.EntityBuilder;
import io.objectbox.model.PropertyFlags;
import io.objectbox.model.PropertyType;


public class MyObjectBox {

    public static BoxStoreBuilder builder() {
        BoxStoreBuilder builder = new BoxStoreBuilder(getModel());
        builder.entity(UniqueGenerated_.__INSTANCE);
        return builder;
    }

    private static byte[] getModel() {
        ModelBuilder modelBuilder = new ModelBuilder();
        modelBuilder.lastEntityId(1, 1699443392711810012L);
        modelBuilder.lastIndexId(2, 8578641540773014588L);
        modelBuilder.lastRelationId(0, 0L);

        buildEntityUniqueGenerated(modelBuilder);

        return modelBuilder.build();
    }

    private static void buildEntityUniqueGenerated(ModelBuilder modelBuilder) {
        EntityBuilder entityBuilder = modelBuilder.entity("UniqueGenerated");
        entityBuilder.id(1, 1699443392711810012L).lastPropertyId(3, 3165064094626578066L);
        entityBuilder.flags(io.objectbox.model.EntityFlags.USE_NO_ARG_CONSTRUCTOR);

        entityBuilder.property("id", PropertyType.Long).id(1, 1304105407891414803L)
                .flags(PropertyFlags.ID | PropertyFlags.NOT_NULL);
        entityBuilder.property("intProp", PropertyType.Int).id(2, 1326107629162952823L)
                .flags(PropertyFlags.NOT_NULL | PropertyFlags.INDEXED | PropertyFlags.UNIQUE).indexId(1, 1990008421926689894L);
        entityBuilder.property("stringProp", PropertyType.String).id(3, 3165064094626578066L)
                .flags(PropertyFlags.INDEXED | PropertyFlags.UNIQUE).indexId(2, 8578641540773014588L);


        entityBuilder.entityDone();
    }

}