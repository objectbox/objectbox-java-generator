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
        modelBuilder.lastEntityId(1, 743912268485545954L);
        modelBuilder.lastIndexId(1, 3609601155488943582L);
        modelBuilder.lastRelationId(0, 0L);

        buildEntityExample(modelBuilder);

        return modelBuilder.build();
    }

    private static void buildEntityExample(ModelBuilder modelBuilder) {
        EntityBuilder entityBuilder = modelBuilder.entity("Example");
        entityBuilder.id(1, 743912268485545954L).lastPropertyId(2, 5165842600627391174L);
        entityBuilder.flags(io.objectbox.model.EntityFlags.USE_NO_ARG_CONSTRUCTOR | io.objectbox.model.EntityFlags.SYNC_ENABLED);

        entityBuilder.property("id", PropertyType.Long).id(1, 2074054563011321890L)
                .flags(PropertyFlags.ID);
        entityBuilder.property("replaceProp", PropertyType.Long).id(2, 5165842600627391174L)
                .flags(PropertyFlags.INDEXED | PropertyFlags.UNIQUE | PropertyFlags.UNIQUE_ON_CONFLICT_REPLACE).indexId(1, 3609601155488943582L);


        entityBuilder.entityDone();
    }

}
