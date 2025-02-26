/*
 * ObjectBox Build Tools
 * Copyright (C) 2024-2025 ObjectBox Ltd.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published
 * by the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package io.objectbox.processor

import io.objectbox.annotation.ExternalType
import org.intellij.lang.annotations.Language
import org.junit.Test


/**
 * Tests the [ExternalType] annotation.
 */
class ExternalTypeTest : BaseProcessorTest() {

    @Test
    fun externalTypeOnProperty_works() {
        // Note: as the plugin has no code or checks specific to each type and testing each type is harder to maintain,
        // test at least the two most common types: Mongo IDs and UUIDs.
        @Language("Java")
        val mongoEntity =
            """
            package com.example;
            import io.objectbox.annotation.Entity;
            import io.objectbox.annotation.ExternalPropertyType;
            import io.objectbox.annotation.ExternalType;
            import io.objectbox.annotation.Id;
            import io.objectbox.relation.ToMany;
            
            @Entity
            public class MongoEntity {
                @Id public long id;
                
                @ExternalType(ExternalPropertyType.MONGO_ID)
                byte[] mongoId;
                
                @ExternalType(ExternalPropertyType.UUID)
                byte[] uuid;
            }
            """.trimIndent()

        val env = TestEnvironment("external-type-mongoId.json")
            .apply { addSourceFile("com.example.MongoEntity", mongoEntity) }

        env.compile()
            .assertThatIt {
                succeededWithoutWarnings()

                // Generated MyObjectBox code should set (correct) externalType
                @Language("Java") val myObjectBox =
                    """     
                    package com.example;
                    
                    import io.objectbox.BoxStore;
                    import io.objectbox.BoxStoreBuilder;
                    import io.objectbox.ModelBuilder;
                    import io.objectbox.ModelBuilder.EntityBuilder;
                    import io.objectbox.model.ExternalPropertyType;
                    import io.objectbox.model.PropertyFlags;
                    import io.objectbox.model.PropertyType;
                    
                    public class MyObjectBox {
                    
                        public static BoxStoreBuilder builder() {
                            BoxStoreBuilder builder = new BoxStoreBuilder(getModel());
                            builder.entity(MongoEntity_.__INSTANCE);
                            return builder;
                        }
                    
                        private static byte[] getModel() {
                            ModelBuilder modelBuilder = new ModelBuilder();
                            modelBuilder.lastEntityId(1, 4896842459897431834L);
                            modelBuilder.lastIndexId(0, 0L);
                            modelBuilder.lastRelationId(3, 4330998623803761847L);
                    
                            buildEntityMongoEntity(modelBuilder);
                    
                            return modelBuilder.build();
                        }
                    
                        private static void buildEntityMongoEntity(ModelBuilder modelBuilder) {
                            EntityBuilder entityBuilder = modelBuilder.entity("MongoEntity");
                            entityBuilder.id(1, 4896842459897431834L).lastPropertyId(3, 638742813187624182L);
                            entityBuilder.flags(io.objectbox.model.EntityFlags.USE_NO_ARG_CONSTRUCTOR);
                    
                            entityBuilder.property("id", PropertyType.Long).id(1, 3717607777155461882L)
                                    .flags(PropertyFlags.ID);
                            entityBuilder.property("mongoId", PropertyType.ByteVector).id(2, 2181682461243454295L)
                                    .externalType(ExternalPropertyType.MongoId);
                            entityBuilder.property("uuid", PropertyType.ByteVector).id(3, 638742813187624182L)
                                    .externalType(ExternalPropertyType.Uuid);
                    
                            entityBuilder.entityDone();
                        }
                    
                    }
                    """.trimIndent()
                generatedSourceFileMatches("com.example.MyObjectBox", myObjectBox)
            }
    }

    @Test
    fun externalTypeOnToMany_works() {
        // Note: as the plugin has no code or checks specific to each type and testing each type is harder to maintain,
        // test at least the two most common types: Mongo IDs and UUIDs.
        @Language("Java")
        val mongoEntity =
            """
            package com.example;
            import io.objectbox.annotation.Entity;
            import io.objectbox.annotation.ExternalPropertyType;
            import io.objectbox.annotation.ExternalType;
            import io.objectbox.annotation.Id;
            import io.objectbox.relation.ToMany;
            
            @Entity
            public class MongoEntity {
                @Id public long id;
                
                @ExternalType(ExternalPropertyType.MONGO_ID_VECTOR)
                public ToMany<MongoEntity> mongoIdEntities;
                
                @ExternalType(ExternalPropertyType.UUID_VECTOR)
                public ToMany<MongoEntity> uuidEntities;
            }
            """.trimIndent()

        val env = TestEnvironment("external-type-tomany-mongoId.json")
            .apply { addSourceFile("com.example.MongoEntity", mongoEntity) }

        env.compile()
            .assertThatIt {
                succeededWithoutWarnings()

                // Generated MyObjectBox code should set (correct) externalType
                @Language("Java") val myObjectBox =
                    """     
                    package com.example;
                    
                    import io.objectbox.BoxStore;
                    import io.objectbox.BoxStoreBuilder;
                    import io.objectbox.ModelBuilder;
                    import io.objectbox.ModelBuilder.EntityBuilder;
                    import io.objectbox.model.ExternalPropertyType;
                    import io.objectbox.model.PropertyFlags;
                    import io.objectbox.model.PropertyType;
                    
                    public class MyObjectBox {
                    
                        public static BoxStoreBuilder builder() {
                            BoxStoreBuilder builder = new BoxStoreBuilder(getModel());
                            builder.entity(MongoEntity_.__INSTANCE);
                            return builder;
                        }
                    
                        private static byte[] getModel() {
                            ModelBuilder modelBuilder = new ModelBuilder();
                            modelBuilder.lastEntityId(1, 7367374710730783876L);
                            modelBuilder.lastIndexId(0, 0L);
                            modelBuilder.lastRelationId(2, 7702572643150505707L);
                    
                            buildEntityMongoEntity(modelBuilder);
                    
                            return modelBuilder.build();
                        }
                    
                        private static void buildEntityMongoEntity(ModelBuilder modelBuilder) {
                            EntityBuilder entityBuilder = modelBuilder.entity("MongoEntity");
                            entityBuilder.id(1, 7367374710730783876L).lastPropertyId(1, 4812746380227816069L);
                            entityBuilder.flags(io.objectbox.model.EntityFlags.USE_NO_ARG_CONSTRUCTOR);
                    
                            entityBuilder.property("id", PropertyType.Long).id(1, 4812746380227816069L)
                                    .flags(PropertyFlags.ID);
                    
                            entityBuilder.relation("mongoIdEntities", 1, 6965793985203435547L, 1, 7367374710730783876L)
                                    .externalType(ExternalPropertyType.MongoIdVector);
                            entityBuilder.relation("uuidEntities", 2, 7702572643150505707L, 1, 7367374710730783876L)
                                    .externalType(ExternalPropertyType.UuidVector);
                    
                            entityBuilder.entityDone();
                        }
                    
                    
                    }
                    """.trimIndent()
                generatedSourceFileMatches("com.example.MyObjectBox", myObjectBox)
            }
    }
}