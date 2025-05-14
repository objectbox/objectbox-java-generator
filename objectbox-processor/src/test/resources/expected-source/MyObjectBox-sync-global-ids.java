package com.example;

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
        builder.entity(Example_.__INSTANCE);
        return builder;
    }

    private static byte[] getModel() {
        ModelBuilder modelBuilder = new ModelBuilder();
        modelBuilder.lastEntityId(1, 507343565663517863L);
        modelBuilder.lastIndexId(0, 0L);
        modelBuilder.lastRelationId(0, 0L);

        buildEntityExample(modelBuilder);

        return modelBuilder.build();
    }

    private static void buildEntityExample(ModelBuilder modelBuilder) {
        EntityBuilder entityBuilder = modelBuilder.entity("Example");
        entityBuilder.id(1, 507343565663517863L).lastPropertyId(1, 1595627190507074139L);
        entityBuilder.flags(io.objectbox.model.EntityFlags.USE_NO_ARG_CONSTRUCTOR | io.objectbox.model.EntityFlags.SYNC_ENABLED | io.objectbox.model.EntityFlags.SHARED_GLOBAL_IDS);

        entityBuilder.property("id", PropertyType.Long).id(1, 1595627190507074139L)
                .flags(PropertyFlags.ID);


        entityBuilder.entityDone();
    }

}
