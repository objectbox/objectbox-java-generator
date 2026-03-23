/*
 * ObjectBox Build Tools
 * Copyright (C) 2026 ObjectBox Ltd.
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
import io.objectbox.model.PropertyFlags
import org.junit.Test

/**
 * Tests related to the `@SyncClock` and `@SyncPrecedence` annotations.
 */
class SyncClockPrecedenceTest : BaseProcessorTest() {

    @Test
    fun syncClock_and_syncPrecedence_setFlags() {
        // language=Java
        val additionalImports = """
        import io.objectbox.annotation.SyncClock;
        import io.objectbox.annotation.SyncPrecedence;
        """

        // language=Java
        val entityBody = """
        @SyncClock
        Long clock;
        
        @SyncPrecedence
        Long precedence;
        """

        // Need stable model file + ids to verify sources match.
        TestEnvironment("sync-precedence.json")
            .apply {
                addEntitySourceFile(
                    sync = true,
                    additionalImports = additionalImports
                ) {
                    entityBody
                }
            }
            .compile()
            .assertThatIt {
                succeededWithoutWarnings()

                // language=Java
                val myObjectBox = """
                package example;
                
                import io.objectbox.BoxStore;
                import io.objectbox.BoxStoreBuilder;
                import io.objectbox.ModelBuilder;
                import io.objectbox.ModelBuilder.EntityBuilder;
                import io.objectbox.model.PropertyFlags;
                import io.objectbox.model.PropertyType;
                
                public class MyObjectBox {
                
                    public static BoxStoreBuilder builder() {
                        BoxStoreBuilder builder = new BoxStoreBuilder(getModel());
                        builder.entity(Example_.__INSTANCE);
                        return builder;
                    }
                
                    private static byte[] getModel() {
                        ModelBuilder modelBuilder = new ModelBuilder();
                        modelBuilder.lastEntityId(1, 1891977718895020850L);
                        modelBuilder.lastIndexId(0, 0L);
                        modelBuilder.lastRelationId(0, 0L);
                
                        buildEntityExample(modelBuilder);
                
                        return modelBuilder.build();
                    }
                
                    private static void buildEntityExample(ModelBuilder modelBuilder) {
                        EntityBuilder entityBuilder = modelBuilder.entity("Example");
                        entityBuilder.id(1, 1891977718895020850L).lastPropertyId(3, 2352657165670805319L);
                        entityBuilder.flags(io.objectbox.model.EntityFlags.USE_NO_ARG_CONSTRUCTOR | io.objectbox.model.EntityFlags.SYNC_ENABLED);
                
                        entityBuilder.property("id", PropertyType.Long).id(1, 6420357617515853065L)
                                .flags(PropertyFlags.ID);
                        entityBuilder.property("clock", PropertyType.Long).id(2, 224717534102795506L)
                                .flags(PropertyFlags.NON_PRIMITIVE_TYPE | PropertyFlags.SYNC_CLOCK);
                        entityBuilder.property("precedence", PropertyType.Long).id(3, 2352657165670805319L)
                                .flags(PropertyFlags.NON_PRIMITIVE_TYPE | PropertyFlags.SYNC_PRECEDENCE);
                
                        entityBuilder.entityDone();
                    }
                
                }
                """.trimIndent()
                generatedSourceFileMatches("example.MyObjectBox", myObjectBox)
            }

        // Use temp model file to assert model file flags.
        val environment = TestEnvironment("not-generated.json", useTemporaryModelFile = true)
            .apply {
                addEntitySourceFile(
                    sync = true,
                    additionalImports = additionalImports
                ) {
                    entityBody
                }
            }
        environment.compile()
            .assertThatIt { succeededWithoutWarnings() }

        val model = environment.readModel()
        val entity = model.findEntity("Example", null)!!

        val clockProp = entity.properties.find { it.name == "clock" }!!
        assertThat(clockProp.flags).isEqualTo(PropertyFlags.SYNC_CLOCK)

        val precedenceProp = entity.properties.find { it.name == "precedence" }!!
        assertThat(precedenceProp.flags).isEqualTo(PropertyFlags.SYNC_PRECEDENCE)
    }

    @Test
    fun syncClock_onEntityWithoutSync_fails() {
        assertAnnotationOnEntityWithoutSync("SyncClock")
    }

    @Test
    fun syncPrecedence_onEntityWithoutSync_fails() {
        assertAnnotationOnEntityWithoutSync("SyncPrecedence")
    }

    private fun assertAnnotationOnEntityWithoutSync(annotation: String) {
        TestEnvironment("not-generated.json", useTemporaryModelFile = true)
            .apply {
                // language=Java
                addEntitySourceFile(
                    additionalImports = "import io.objectbox.annotation.$annotation;"
                ) {
                    """
                    @$annotation
                    Long field;
                    """
                }
            }
            .compile()
            .assertThatIt {
                failed()
                hadErrorContaining(
                    "@$annotation can only be used on a property of a synced entity (annotated with @Sync)"
                )
            }
    }

    @Test
    fun syncClock_multipleTimes_fails() {
        assertDuplicateAnnotationOnEntityFails("SyncClock")
    }

    @Test
    fun syncPrecedence_multipleTimes_fails() {
        assertDuplicateAnnotationOnEntityFails("SyncPrecedence")
    }

    private fun assertDuplicateAnnotationOnEntityFails(annotation: String) {
        TestEnvironment("not-generated.json", useTemporaryModelFile = true)
            .apply {
                // language=Java
                addEntitySourceFile(
                    sync = true,
                    additionalImports = "import io.objectbox.annotation.$annotation;"
                ) {
                    """
                    @$annotation
                    Long field1;
        
                    @$annotation
                    Long field2;
                    """
                }
            }
            .compile()
            .assertThatIt {
                failed()
                hadErrorContaining(
                    "only one property can be annotated with @$annotation, but found multiple"
                )
            }
    }

    @Test
    fun syncClock_and_syncPrecedence_onSameProperty_fails() {
        TestEnvironment("not-generated.json", useTemporaryModelFile = true)
            .apply {
                // language=Java
                addEntitySourceFile(
                    sync = true,
                    additionalImports =
                        """
                        import io.objectbox.annotation.SyncClock;
                        import io.objectbox.annotation.SyncPrecedence;
                        """
                ) {
                    """
                    @SyncClock
                    @SyncPrecedence
                    Long clockAndPrecedence;
                    """
                }
            }
            .compile()
            .assertThatIt {
                failed()
                hadErrorContaining(
                    "@SyncClock and @SyncPrecedence cannot be used on the same property"
                )
            }
    }

    @Test
    fun syncClock_onNonLongProperty_fails() {
        assertAnnotationOnNonLongPropertyFails("SyncClock")
    }

    @Test
    fun syncPrecedence_onNonLongProperty_fails() {
        assertAnnotationOnNonLongPropertyFails("SyncPrecedence")
    }

    private fun assertAnnotationOnNonLongPropertyFails(annotation: String) {
        TestEnvironment("not-generated.json", useTemporaryModelFile = true)
            .apply {
                // language=Java
                addEntitySourceFile(
                    sync = true,
                    additionalImports = "import io.objectbox.annotation.$annotation;"
                ) {
                    """
                    @$annotation
                    String field;
                    """
                }
            }
            .compile()
            .assertThatIt {
                failed()
                hadErrorContaining(
                    "@$annotation can only be used on Long (OBXPropertyType.Long) properties"
                )
            }
    }
}