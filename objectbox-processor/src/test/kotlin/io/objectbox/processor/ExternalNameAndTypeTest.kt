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

import io.objectbox.annotation.ExternalName
import io.objectbox.annotation.ExternalType
import org.intellij.lang.annotations.Language
import org.junit.Test


/**
 * Tests the [ExternalName] and [ExternalType] annotations.
 */
class ExternalNameAndTypeTest : BaseProcessorTest() {

    // Note: the annotation processor doesn't have checks for types (done at runtime by the database),
    // so just verify parsing works and generated code is as expected.
    @Test
    fun externalNameAndType_parsesAndGeneratesAsExpected() {
        @Language("Java")
        val mongoEntity =
            """
            package com.example;
            import io.objectbox.annotation.Entity;
            import io.objectbox.annotation.ExternalName;
            import io.objectbox.annotation.ExternalPropertyType;
            import io.objectbox.annotation.ExternalType;
            import io.objectbox.annotation.Id;
            import io.objectbox.relation.ToMany;
            
            @Entity
            @ExternalName("obx-test-external-entity")
            public class MongoEntity {
                @Id public long id;
                
                @ExternalName("obx-test-external-property")
                @ExternalType(ExternalPropertyType.UUID)
                public byte[] externalId;
                
                @ExternalName("obx-test-external-tomany")
                @ExternalType(ExternalPropertyType.UUID_VECTOR)
                public ToMany<MongoEntity> externalIds;
            }
            """.trimIndent()

        val env = TestEnvironment("external-type-and-name.json")
            .apply { addSourceFile("com.example.MongoEntity", mongoEntity) }

        env.compile()
            .assertThatIt {
                succeededWithoutWarnings()

                // Generated MyObjectBox code should set (correct) external type and name
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
                            modelBuilder.lastRelationId(1, 2631459366490152667L);
                    
                            buildEntityMongoEntity(modelBuilder);
                    
                            return modelBuilder.build();
                        }
                    
                        private static void buildEntityMongoEntity(ModelBuilder modelBuilder) {
                            EntityBuilder entityBuilder = modelBuilder.entity("MongoEntity");
                            entityBuilder.externalName("obx-test-external-entity");
                            entityBuilder.id(1, 4896842459897431834L).lastPropertyId(2, 2181682461243454295L);
                            entityBuilder.flags(io.objectbox.model.EntityFlags.USE_NO_ARG_CONSTRUCTOR);
                    
                            entityBuilder.property("id", PropertyType.Long)
                                    .id(1, 3717607777155461882L)
                                    .flags(PropertyFlags.ID);
                            entityBuilder.property("externalId", PropertyType.ByteVector)
                                    .id(2, 2181682461243454295L)
                                    .externalName("obx-test-external-property")
                                    .externalType(ExternalPropertyType.Uuid);
                                                        
                            entityBuilder.relation("externalIds", 1, 2631459366490152667L, 1, 4896842459897431834L)
                                    .externalName("obx-test-external-tomany")
                                    .externalType(ExternalPropertyType.UuidVector);                                                                                                
                    
                            entityBuilder.entityDone();
                        }
                    
                    }
                    """.trimIndent()
                generatedSourceFileMatches("com.example.MyObjectBox", myObjectBox)
            }
    }

}