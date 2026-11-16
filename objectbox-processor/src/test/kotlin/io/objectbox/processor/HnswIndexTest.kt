/*
 * ObjectBox Build Tools
 * Copyright (C) 2024-2026 ObjectBox Ltd.
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
import io.objectbox.annotation.VectorBuildLevel
import io.objectbox.annotation.VectorDistanceType
import io.objectbox.annotation.VectorQuantization
import io.objectbox.annotation.VectorSearchLevel
import io.objectbox.model.PropertyFlags
import org.intellij.lang.annotations.Language
import org.junit.Test

/**
 * Tests the [HnswIndex] annotation.
 */
class HnswIndexTest(processorType: ProcessorType) : BaseProcessorTest(processorType) {

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

        val env = env("hnsw-unsupported.json", useTemporaryModelFile = true)
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

        val env = env("hnsw-defaults.json")
            .apply { addSourceFile("com.example.City", citySource) }

        env.compile()
            .assertThatIt {
                succeededWithoutWarnings()

                // Generated MyObjectBox code should only set dimensions
                @Language("Java") val myObjectBox =
                    """
                    package com.example;

                    import io.objectbox.BoxStore;
                    import io.objectbox.BoxStoreBuilder;
                    import io.objectbox.HnswIndexParams;
                    import io.objectbox.ModelBuilder;
                    import io.objectbox.ModelBuilder.EntityBuilder;
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
                                    .hnswParams(new HnswIndexParams(2));
                    
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
        val hnswParams = vectorProperty.hnswParams!!
        assertThat(hnswParams.dimensions).isEqualTo(2)
        assertThat(hnswParams.neighborsPerNode).isNull()
        assertThat(hnswParams.indexingSearchCount).isNull()
        assertThat(hnswParams.flagsExpressionSet).isEmpty()
        assertThat(hnswParams.distanceType).isEqualTo(VectorDistanceType.DEFAULT)
        assertThat(hnswParams.reparationBacklinkProbability).isNull()
        assertThat(hnswParams.vectorCacheHintSizeKb).isNull()
        assertThat(hnswParams.quantization).isEqualTo(VectorQuantization.DEFAULT)
        assertThat(hnswParams.quantizationOversample).isNull()
        assertThat(hnswParams.quantizationRescoreFactor).isNull()
        assertThat(hnswParams.searchLevel).isEqualTo(VectorSearchLevel.DEFAULT)
        assertThat(hnswParams.buildLevel).isEqualTo(VectorBuildLevel.DEFAULT)
        assertThat(hnswParams.bruteForceMaxCandidates).isNull()
        assertThat(hnswParams.filteredSearchMaxNodes).isNull()
    }

    /**
     * Sets all parameters, with dimensions late init allows other parameters.
     */
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
            import io.objectbox.annotation.VectorBuildLevel;
            import io.objectbox.annotation.VectorDistanceType;
            import io.objectbox.annotation.VectorQuantization;
            import io.objectbox.annotation.VectorSearchLevel;

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
                                lateInit = true),
                        distanceType = VectorDistanceType.EUCLIDEAN,
                        reparationBacklinkProbability = 0.95F,
                        vectorCacheHintSizeKB = 2097152,
                        quantization = VectorQuantization.SCALAR_QUANT_8,
                        quantizationOversample = 2.0F,
                        quantizationRescoreFactor = 1.5F,
                        searchLevel = VectorSearchLevel.L6_QUALITY,
                        buildLevel = VectorBuildLevel.L5_BALANCED_QUALITY,
                        bruteForceMaxCandidates = 5000,
                        filteredSearchMaxNodes = 50000
                )
                float[] coordinates;
            }
            """.trimIndent()
        val env = env("hnsw-defaults.json")
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
                    import io.objectbox.HnswIndexParams;
                    import io.objectbox.ModelBuilder;
                    import io.objectbox.ModelBuilder.EntityBuilder;
                    import io.objectbox.annotation.VectorBuildLevel;
                    import io.objectbox.annotation.VectorDistanceType;
                    import io.objectbox.annotation.VectorQuantization;
                    import io.objectbox.annotation.VectorSearchLevel;
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
                                    .hnswParams(new HnswIndexParams(2)
                                            .neighborsPerNode(30L)
                                            .indexingSearchCount(100L)
                                            .flags(HnswFlags.DebugLogs | HnswFlags.DebugLogsDetailed | HnswFlags.VectorCacheSimdPaddingOff | HnswFlags.LateInit)
                                            .distanceType(VectorDistanceType.EUCLIDEAN)
                                            .reparationBacklinkProbability(0.95F)
                                            .vectorCacheHintSizeKb(2097152L)
                                            .quantization(VectorQuantization.SCALAR_QUANT_8)
                                            .quantizationOversample(2.0F)
                                            .quantizationRescoreFactor(1.5F)
                                            .searchLevel(VectorSearchLevel.L6_QUALITY)
                                            .buildLevel(VectorBuildLevel.L5_BALANCED_QUALITY)
                                            .bruteForceMaxCandidates(5000L)
                                            .filteredSearchMaxNodes(50000L));
                    
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
        val hnswParams = vectorProperty.hnswParams!!
        assertThat(hnswParams.dimensions).isEqualTo(2)
        assertThat(hnswParams.neighborsPerNode).isEqualTo(30)
        assertThat(hnswParams.indexingSearchCount).isEqualTo(100)
        assertThat(hnswParams.flagsExpressionSet).containsExactly(
            "HnswFlags.DebugLogs",
            "HnswFlags.DebugLogsDetailed",
            "HnswFlags.VectorCacheSimdPaddingOff",
            "HnswFlags.LateInit"
        )
        assertThat(hnswParams.distanceType).isEqualTo(VectorDistanceType.EUCLIDEAN)
        assertThat(hnswParams.reparationBacklinkProbability).isEqualTo(0.95F)
        assertThat(hnswParams.vectorCacheHintSizeKb).isEqualTo(2097152)
        assertThat(hnswParams.quantization).isEqualTo(VectorQuantization.SCALAR_QUANT_8)
        assertThat(hnswParams.quantizationOversample).isEqualTo(2.0F)
        assertThat(hnswParams.quantizationRescoreFactor).isEqualTo(1.5F)
        assertThat(hnswParams.searchLevel).isEqualTo(VectorSearchLevel.L6_QUALITY)
        assertThat(hnswParams.buildLevel).isEqualTo(VectorBuildLevel.L5_BALANCED_QUALITY)
        assertThat(hnswParams.bruteForceMaxCandidates).isEqualTo(5000)
        assertThat(hnswParams.filteredSearchMaxNodes).isEqualTo(50000)
    }

    /**
     * Only imports the classes required by the set parameters.
     */
    @Test
    fun hnswAnnotation_importsOnlyUsedClasses() {
        @Language("Java")
        val citySource =
            """
            package com.example;
            import io.objectbox.annotation.Entity;
            import io.objectbox.annotation.Id;
            import io.objectbox.annotation.HnswIndex;
            import io.objectbox.annotation.VectorSearchLevel;

            @Entity
            public class City {
                @Id public long id;

                @HnswIndex(dimensions = 2, searchLevel = VectorSearchLevel.L6_QUALITY)
                float[] coordinates;
            }
            """.trimIndent()

        val env = env("hnsw-defaults.json")
            .apply { addSourceFile("com.example.City", citySource) }

        env.compile()
            .assertThatIt {
                succeededWithoutWarnings()
                val myObjectBox = generatedSourceFileContent("com.example.MyObjectBox")
                assertThat(myObjectBox).contains(".hnswParams(new HnswIndexParams(2)")
                assertThat(myObjectBox).contains(".searchLevel(VectorSearchLevel.L6_QUALITY));")
                assertThat(myObjectBox).contains("import io.objectbox.annotation.VectorSearchLevel;")
                assertThat(myObjectBox).doesNotContain("import io.objectbox.annotation.VectorQuantization;")
                assertThat(myObjectBox).doesNotContain("import io.objectbox.model.HnswFlags;")
                assertThat(myObjectBox).doesNotContain("import io.objectbox.model.HnswDistanceType;")
            }
    }

    @Test
    fun hnswAnnotation_quantizationWithGeo_errors() {
        assertHnswIndexError(
            "@HnswIndex(dimensions = 2, distanceType = VectorDistanceType.GEO," +
                    " quantization = VectorQuantization.TURBO_QUANT_4)",
            "quantization TURBO_QUANT_4 is not supported with distanceType GEO, use NONE or DEFAULT"
        )
    }

    @Test
    fun hnswAnnotation_noQuantizationWithGeo_works() {
        @Language("Java")
        val citySource =
            """
            package com.example;
            import io.objectbox.annotation.Entity;
            import io.objectbox.annotation.Id;
            import io.objectbox.annotation.HnswIndex;
            import io.objectbox.annotation.VectorDistanceType;
            import io.objectbox.annotation.VectorQuantization;

            @Entity
            public class City {
                @Id public long id;

                @HnswIndex(dimensions = 2, distanceType = VectorDistanceType.GEO, quantization = VectorQuantization.NONE)
                float[] coordinates;
            }
            """.trimIndent()

        val env = env("hnsw-defaults.json")
            .apply { addSourceFile("com.example.City", citySource) }

        env.compile()
            .assertThatIt {
                succeededWithoutWarnings()
                val myObjectBox = generatedSourceFileContent("com.example.MyObjectBox")
                assertThat(myObjectBox).contains(".hnswParams(new HnswIndexParams(2)")
                assertThat(myObjectBox).contains(".distanceType(VectorDistanceType.GEO)")
                assertThat(myObjectBox).contains(".quantization(VectorQuantization.NONE));")
            }
    }

    @Test
    fun hnswAnnotation_lateInitWithoutDimensions_works() {
        @Language("Java")
        val citySource =
            """
            package com.example;
            import io.objectbox.annotation.Entity;
            import io.objectbox.annotation.Id;
            import io.objectbox.annotation.HnswIndex;
            import io.objectbox.annotation.HnswFlags;
            
            @Entity
            public class City {
                @Id public long id;
                
                @HnswIndex(flags = @HnswFlags(lateInit = true))
                float[] coordinates;
            }    
            """.trimIndent()

        val env = env("hnsw-defaults.json")
            .apply { addSourceFile("com.example.City", citySource) }

        env.compile()
            .assertThatIt {
                succeededWithoutWarnings()
                val myObjectBox = generatedSourceFileContent("com.example.MyObjectBox")
                assertThat(myObjectBox).contains(".hnswParams(new HnswIndexParams(0)")
                assertThat(myObjectBox).contains(".flags(HnswFlags.LateInit));")
            }
    }

    @Test
    fun hnswAnnotation_noDimensionsWithoutLateInit_errors() {
        assertHnswIndexError(
            "@HnswIndex(distanceType = VectorDistanceType.COSINE)",
            "dimensions must be 1 or greater (or use late init: @HnswFlags(lateInit = true))"
        )
    }

    /**
     * Without dimensions, late init takes all parameters from BoxStore.initHnswIndex(), so the database would ignore
     * the other ones of the annotation.
     */
    @Test
    fun hnswAnnotation_lateInitWithoutDimensionsWithParams_errors() {
        assertHnswIndexError(
            "@HnswIndex(distanceType = VectorDistanceType.COSINE, quantization = VectorQuantization.NONE," +
                    " flags = @HnswFlags(lateInit = true, debugLogs = true))",
            "late init without dimensions takes the parameters from BoxStore.initHnswIndex() only," +
                    " set the dimensions or remove these: distanceType, quantization, debugLogs"
        )
        assertHnswIndexError(
            "@HnswIndex(reparationBacklinkProbability = 0.5F, flags = @HnswFlags(lateInit = true))",
            "set the dimensions or remove these: reparationBacklinkProbability"
        )
    }

    @Test
    fun hnswAnnotation_invalidParamsSince6_errors() {
        assertHnswIndexError(
            "@HnswIndex(dimensions = 2, quantizationOversample = 0.5F)",
            "quantizationOversample must be 0 or from 1 to 1000000"
        )
        assertHnswIndexError(
            "@HnswIndex(dimensions = 2, quantizationOversample = Float.POSITIVE_INFINITY)",
            "quantizationOversample must be 0 or from 1 to 1000000"
        )
        assertHnswIndexError(
            "@HnswIndex(dimensions = 2, quantizationRescoreFactor = 0.5F)",
            "quantizationRescoreFactor must be 0 or from 1 to 1000000"
        )
        assertHnswIndexError(
            "@HnswIndex(dimensions = 2, quantizationRescoreFactor = 1000001F)",
            "quantizationRescoreFactor must be 0 or from 1 to 1000000"
        )
        assertHnswIndexError(
            "@HnswIndex(dimensions = 2, bruteForceMaxCandidates = -1)",
            "bruteForceMaxCandidates must not be negative"
        )
        assertHnswIndexError(
            "@HnswIndex(dimensions = 2, bruteForceMaxCandidates = 4294967296L)",
            "bruteForceMaxCandidates must be at most 4294967295"
        )
        assertHnswIndexError(
            "@HnswIndex(dimensions = 2, filteredSearchMaxNodes = -1)",
            "filteredSearchMaxNodes must not be negative"
        )
        assertHnswIndexError(
            "@HnswIndex(dimensions = 2, filteredSearchMaxNodes = 4294967296L)",
            "filteredSearchMaxNodes must be at most 4294967295"
        )
    }

    @Test
    fun hnswAnnotation_valuesBeyond32Bits_errors() {
        assertHnswIndexError(
            "@HnswIndex(dimensions = 4294967296L)",
            "dimensions must be at most 4294967295"
        )
        assertHnswIndexError(
            "@HnswIndex(dimensions = 2, neighborsPerNode = 4294967296L)",
            "neighborsPerNode must be at most 4294967295"
        )
        assertHnswIndexError(
            "@HnswIndex(dimensions = 2, indexingSearchCount = 4294967296L)",
            "indexingSearchCount must be at most 4294967295"
        )
    }

    /**
     * The database ignores the deprecated reparationLimitCandidates flag since 6.0.
     */
    @Test
    fun hnswAnnotation_reparationLimitCandidates_errors() {
        assertHnswIndexError(
            "@HnswIndex(dimensions = 2, flags = @HnswFlags(reparationLimitCandidates = true))",
            "reparationLimitCandidates is no longer supported, remove it"
        )
    }

    private fun assertHnswIndexError(annotation: String, expectedError: String) {
        @Language("Java")
        val citySource =
            """
            package com.example;
            import io.objectbox.annotation.Entity;
            import io.objectbox.annotation.Id;
            import io.objectbox.annotation.HnswFlags;
            import io.objectbox.annotation.HnswIndex;
            import io.objectbox.annotation.VectorDistanceType;
            import io.objectbox.annotation.VectorQuantization;

            @Entity
            public class City {
                @Id public long id;

                $annotation
                float[] coordinates;
            }    
            """.trimIndent()

        val env = env("hnsw-invalid.json", useTemporaryModelFile = true)
            .apply { addSourceFile("com.example.City", citySource) }

        env.compile()
            .assertThatIt {
                failed()
                hadErrorContaining(expectedError)
            }
    }

}
