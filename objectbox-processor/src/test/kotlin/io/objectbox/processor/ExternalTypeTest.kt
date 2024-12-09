/*
 * ObjectBox Build Tools
 * Copyright (C) 2024 ObjectBox Ltd.
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

import com.google.common.truth.Truth.assertThat
import io.objectbox.annotation.ExternalType
import io.objectbox.model.ExternalPropertyType
import org.intellij.lang.annotations.Language
import org.junit.Test


/**
 * Tests the [ExternalType] annotation.
 */
class ExternalTypeTest : BaseProcessorTest() {

    @Test
    fun mongoId() {
        @Language("Java")
        val mongoIdSource =
            """
            package com.example;
            import io.objectbox.annotation.Entity;
            import io.objectbox.annotation.ExternalPropertyType;
            import io.objectbox.annotation.ExternalType;
            import io.objectbox.annotation.Id;
            
            @Entity
            public class MongoEntity {
                @Id public long id;
                
                @ExternalType(ExternalPropertyType.MONGO_ID)
                byte[] mongoId;
            }    
            """.trimIndent()

        val env = TestEnvironment("external-type-mongoId.json")
            .apply { addSourceFile("com.example.MongoEntity", mongoIdSource) }

        env.compile()
            .assertThatIt {
                succeededWithoutWarnings()

                // Generated MyObjectBox code should set externalType
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
                            modelBuilder.lastRelationId(0, 0L);
                    
                            buildEntityMongoEntity(modelBuilder);
                    
                            return modelBuilder.build();
                        }
                    
                        private static void buildEntityMongoEntity(ModelBuilder modelBuilder) {
                            EntityBuilder entityBuilder = modelBuilder.entity("MongoEntity");
                            entityBuilder.id(1, 4896842459897431834L).lastPropertyId(2, 2181682461243454295L);
                            entityBuilder.flags(io.objectbox.model.EntityFlags.USE_NO_ARG_CONSTRUCTOR);
                    
                            entityBuilder.property("id", PropertyType.Long).id(1, 3717607777155461882L)
                                    .flags(PropertyFlags.ID);
                            entityBuilder.property("mongoId", PropertyType.ByteVector).id(2, 2181682461243454295L)
                                    .externalType(ExternalPropertyType.MongoId);
                    
                            entityBuilder.entityDone();
                        }
                    
                    }
                    """.trimIndent()
                generatedSourceFileMatches("com.example.MyObjectBox", myObjectBox)
            }

        val mongoEntityModel = env.schema.entities[0]
        val externalProperty = mongoEntityModel.properties.find { it.propertyName == "mongoId" }!!
        assertThat(externalProperty.externalTypeId).isEqualTo(ExternalPropertyType.MongoId)
    }

}