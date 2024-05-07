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
import io.objectbox.annotation.HnswIndex
import io.objectbox.model.PropertyFlags
import org.intellij.lang.annotations.Language
import org.junit.Test

/**
 * Tests the [HnswIndex] annotation.
 */
class HnswIndexTest : BaseProcessorTest() {

    @Test
    fun hnswAnnotation_notFloatArray_errors() {
        @Language("Java")
        val citySource =
            """
            package com.example;
            import io.objectbox.annotation.Entity;
            import io.objectbox.annotation.Id;
            import io.objectbox.annotation.HnswIndex;
            
            @Entity
            public class City {
                @Id public long id;
                
                @HnswIndex(dimensions = 2)
                double[] coordinates;
            }    
            """.trimIndent()

        val env = TestEnvironment("hnsw-unsupported.json", useTemporaryModelFile = true)
            .apply { addSourceFile("com.example.City", citySource) }

        env.compile()
            .assertThatIt {
                failed()
                hadErrorContaining("@HnswIndex is only supported for float vector properties.")
            }
    }

    @Test
    fun hnswAnnotation_defaults() {
        @Language("Java")
        val citySource =
            """
            package com.example;
            import io.objectbox.annotation.Entity;
            import io.objectbox.annotation.Id;
            import io.objectbox.annotation.HnswIndex;
            
            @Entity
            public class City {
                @Id public long id;
                
                @HnswIndex(dimensions = 2)
                float[] coordinates;
            }    
            """.trimIndent()

        val env = TestEnvironment("hnsw-defaults.json")
            .apply { addSourceFile("com.example.City", citySource) }

        env.compile()
            .assertThatIt {
                succeededWithoutWarnings()

                // Generated MyObjectBox code should use all null values
                @Language("Java") val myObjectBox =
                    """     
                    package com.example;
                          
                    import io.objectbox.BoxStore;
                    import io.objectbox.BoxStoreBuilder;
                    import io.objectbox.ModelBuilder;
                    import io.objectbox.ModelBuilder.EntityBuilder;
                    import io.objectbox.model.HnswDistanceType;
                    import io.objectbox.model.HnswFlags;
                    import io.objectbox.model.PropertyFlags;
                    import io.objectbox.model.PropertyType;
                    
                    public class MyObjectBox {
                    
                        public static BoxStoreBuilder builder() {
                            BoxStoreBuilder builder = new BoxStoreBuilder(getModel());
                            builder.entity(City_.__INSTANCE);
                            return builder;
                        }
                    
                        private static byte[] getModel() {
                            ModelBuilder modelBuilder = new ModelBuilder();
                            modelBuilder.lastEntityId(1, 4502811246158447523L);
                            modelBuilder.lastIndexId(1, 2826677083708064981L);
                            modelBuilder.lastRelationId(0, 0L);
                    
                            buildEntityCity(modelBuilder);
                    
                            return modelBuilder.build();
                        }
                    
                        private static void buildEntityCity(ModelBuilder modelBuilder) {
                            EntityBuilder entityBuilder = modelBuilder.entity("City");
                            entityBuilder.id(1, 4502811246158447523L).lastPropertyId(2, 5548164134743129791L);
                            entityBuilder.flags(io.objectbox.model.EntityFlags.USE_NO_ARG_CONSTRUCTOR);
                    
                            entityBuilder.property("id", PropertyType.Long).id(1, 5973239523867277109L)
                                    .flags(PropertyFlags.ID);
                            entityBuilder.property("coordinates", PropertyType.FloatVector).id(2, 5548164134743129791L)
                                    .flags(PropertyFlags.INDEXED).indexId(1, 2826677083708064981L)
                                    .hnswParams(2, null, null, null, null, null, null);
                    
                            entityBuilder.entityDone();
                        }
                    
                    }
                    """.trimIndent()
                generatedSourceFileMatches("com.example.MyObjectBox", myObjectBox)
            }

        val cityModel = env.schema.entities[0]
        val vectorProperty = cityModel.properties.find { it.propertyName == "coordinates" }!!
        assertThat(vectorProperty.index).isNotNull()
        assertThat(vectorProperty.index!!.indexFlags).isEqualTo(PropertyFlags.INDEXED)
        assertThat(vectorProperty.hnswParams).isNotNull()
        assertThat(vectorProperty.hnswParams!!.dimensions).isEqualTo(2)
        assertThat(vectorProperty.hnswParams!!.neighborsPerNode).isNull()
        assertThat(vectorProperty.hnswParams!!.indexingSearchCount).isNull()
        assertThat(vectorProperty.hnswParams!!.flagsExpressionSet).isEmpty()
        assertThat(vectorProperty.hnswParams!!.distanceTypeExpression).isNull()
        assertThat(vectorProperty.hnswParams!!.reparationBacklinkProbability).isNull()
        assertThat(vectorProperty.hnswParams!!.vectorCacheHintSizeKb).isNull()
    }

    @Test
    fun hnswAnnotation_allProperties() {

        @Language("Java")
        val citySource =
            """
            package com.example;
            import io.objectbox.annotation.Entity;
            import io.objectbox.annotation.Id;
            import io.objectbox.annotation.HnswIndex;
            import io.objectbox.annotation.HnswFlags;
            import io.objectbox.annotation.VectorDistanceType;
            
            @Entity
            public class City {
                @Id public long id;
                
                @HnswIndex(
                        dimensions = 2,
                        neighborsPerNode = 30,
                        indexingSearchCount = 100,
                        flags = @HnswFlags(
                                debugLogs = true,
                                debugLogsDetailed = true,
                                vectorCacheSimdPaddingOff = true,
                                reparationLimitCandidates = true),
                        distanceType = VectorDistanceType.EUCLIDEAN,
                        reparationBacklinkProbability = 0.95F,
                        vectorCacheHintSizeKB = 2097152
                )
                float[] coordinates;
            }    
            """.trimIndent()
        val env = TestEnvironment("hnsw-defaults.json")
            .apply { addSourceFile("com.example.City", citySource) }

        env.compile()
            .assertThatIt {
                succeededWithoutWarnings()

                // Generated MyObjectBox code should use constants, add HNSW related imports
                @Language("Java") val myObjectBox =
                    """     
                    package com.example;
                          
                    import io.objectbox.BoxStore;
                    import io.objectbox.BoxStoreBuilder;
                    import io.objectbox.ModelBuilder;
                    import io.objectbox.ModelBuilder.EntityBuilder;
                    import io.objectbox.model.HnswDistanceType;
                    import io.objectbox.model.HnswFlags;
                    import io.objectbox.model.PropertyFlags;
                    import io.objectbox.model.PropertyType;
                    
                    public class MyObjectBox {
                    
                        public static BoxStoreBuilder builder() {
                            BoxStoreBuilder builder = new BoxStoreBuilder(getModel());
                            builder.entity(City_.__INSTANCE);
                            return builder;
                        }
                    
                        private static byte[] getModel() {
                            ModelBuilder modelBuilder = new ModelBuilder();
                            modelBuilder.lastEntityId(1, 4502811246158447523L);
                            modelBuilder.lastIndexId(1, 2826677083708064981L);
                            modelBuilder.lastRelationId(0, 0L);
                    
                            buildEntityCity(modelBuilder);
                    
                            return modelBuilder.build();
                        }
                    
                        private static void buildEntityCity(ModelBuilder modelBuilder) {
                            EntityBuilder entityBuilder = modelBuilder.entity("City");
                            entityBuilder.id(1, 4502811246158447523L).lastPropertyId(2, 5548164134743129791L);
                            entityBuilder.flags(io.objectbox.model.EntityFlags.USE_NO_ARG_CONSTRUCTOR);
                    
                            entityBuilder.property("id", PropertyType.Long).id(1, 5973239523867277109L)
                                    .flags(PropertyFlags.ID);
                            entityBuilder.property("coordinates", PropertyType.FloatVector).id(2, 5548164134743129791L)
                                    .flags(PropertyFlags.INDEXED).indexId(1, 2826677083708064981L)
                                    .hnswParams(2, 30L, 100L, HnswFlags.DebugLogs | HnswFlags.DebugLogsDetailed | HnswFlags.VectorCacheSimdPaddingOff | HnswFlags.ReparationLimitCandidates, HnswDistanceType.Euclidean, 0.95F, 2097152L);
                    
                            entityBuilder.entityDone();
                        }
                    
                    }
                    """.trimIndent()
                generatedSourceFileMatches("com.example.MyObjectBox", myObjectBox)
            }

        val cityModel = env.schema.entities[0]
        val vectorProperty = cityModel.properties.find { it.propertyName == "coordinates" }!!
        assertThat(vectorProperty.index).isNotNull()
        assertThat(vectorProperty.index!!.indexFlags).isEqualTo(PropertyFlags.INDEXED)
        assertThat(vectorProperty.hnswParams).isNotNull()
        assertThat(vectorProperty.hnswParams!!.dimensions).isEqualTo(2)
        assertThat(vectorProperty.hnswParams!!.neighborsPerNode).isEqualTo(30)
        assertThat(vectorProperty.hnswParams!!.indexingSearchCount).isEqualTo(100)
        assertThat(vectorProperty.hnswParams!!.flagsExpressionSet).containsExactly(
            "HnswFlags.DebugLogs",
            "HnswFlags.DebugLogsDetailed",
            "HnswFlags.VectorCacheSimdPaddingOff",
            "HnswFlags.ReparationLimitCandidates"
        )
        assertThat(vectorProperty.hnswParams!!.distanceTypeExpression).isEqualTo("HnswDistanceType.Euclidean")
        assertThat(vectorProperty.hnswParams!!.reparationBacklinkProbability).isEqualTo(0.95F)
        assertThat(vectorProperty.hnswParams!!.vectorCacheHintSizeKb).isEqualTo(2097152)
    }

}