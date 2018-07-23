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
        builder.entity(IndexGenerated_.__INSTANCE);
        return builder;
    }

    private static byte[] getModel() {
        ModelBuilder modelBuilder = new ModelBuilder();
        modelBuilder.lastEntityId(1, 7084431515111534289L);
        modelBuilder.lastIndexId(5, 7832271055854174317L);
        modelBuilder.lastRelationId(0, 0L);

        EntityBuilder entityBuilder;

        entityBuilder = modelBuilder.entity("IndexGenerated");
        entityBuilder.id(1, 7084431515111534289L).lastPropertyId(6, 7923177323346835759L);
        entityBuilder.flags(io.objectbox.model.EntityFlags.USE_NO_ARG_CONSTRUCTOR);
        entityBuilder.property("id", PropertyType.Long).id(1, 5977303612346588151L)
                .flags(PropertyFlags.ID | PropertyFlags.NOT_NULL);
        entityBuilder.property("intProp", PropertyType.Int).id(2, 4686331430971777646L)
                .flags(PropertyFlags.NOT_NULL | PropertyFlags.INDEXED).indexId(1, 1174449523189016795L);
        entityBuilder.property("boolProp", PropertyType.Bool).id(3, 8467822549470294700L)
                .flags(PropertyFlags.NOT_NULL | PropertyFlags.INDEXED).indexId(2, 983663037912438369L);
        entityBuilder.property("stringProp", PropertyType.String).id(4, 6533786315119600269L)
                .flags(PropertyFlags.INDEX_HASH).indexId(3, 1891898801773255310L);
        entityBuilder.property("dateProp", PropertyType.Date).id(5, 5827784785326621795L)
                .flags(PropertyFlags.INDEX_HASH64).indexId(4, 2061695745452259841L);
        entityBuilder.entityDone();

        return modelBuilder.build();
    }

}