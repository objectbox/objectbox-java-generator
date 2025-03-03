/*
 * ObjectBox Build Tools
 * Copyright (C) 2023-2024 ObjectBox Ltd.
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

import org.intellij.lang.annotations.Language
import org.junit.Test


class ConvertTest : BaseProcessorTest() {

    @Test
    fun toStringArray() {
        @Language("Java")
        val stringListEntity =
            """
            package com.example;
            import java.util.List;
            import io.objectbox.annotation.Convert;
            import io.objectbox.annotation.Entity;
            import io.objectbox.annotation.Id;
                
            @Entity
            public class StringListEntity {
                @Id long id;
                
                @Convert(converter = StringListConverter.class, dbType = String[].class)
                List<String> stringList;
               
            }
            """.trimIndent()

        @Language("Java")
        val stringListConverter =
            """
            package com.example;
            import java.util.List;
            import io.objectbox.converter.PropertyConverter;
            
            public class StringListConverter implements PropertyConverter<List<String>, String[]> {
                @Override
                public List<String> convertToEntityProperty(String[] databaseValue) {
                    return null;
                }
                @Override
                public String[] convertToDatabaseValue(List<String> entityProperty) {
                    return null;
                }
            }        
            """.trimIndent()

        TestEnvironment("convert-to-string-array.json", useTemporaryModelFile = true)
            .apply {
                addSourceFile("com.example.StringListEntity", stringListEntity)
                addSourceFile("com.example.StringListConverter", stringListConverter)
            }
            .compile()
            .assertThatIt {
                succeededWithoutWarnings()

                @Language("Java")
                val expectedCursor =
                    """
                    package com.example;
                    
                    import io.objectbox.BoxStore;
                    import io.objectbox.Cursor;
                    import io.objectbox.annotation.apihint.Internal;
                    import io.objectbox.internal.CursorFactory;
                    import java.util.List;
                            
                    public final class StringListEntityCursor extends Cursor<StringListEntity> {
                        @Internal
                        static final class Factory implements CursorFactory<StringListEntity> {
                            @Override
                            public Cursor<StringListEntity> createCursor(io.objectbox.Transaction tx, long cursorHandle, BoxStore boxStoreForEntities) {
                                return new StringListEntityCursor(tx, cursorHandle, boxStoreForEntities);
                            }
                        }
                    
                        private static final StringListEntity_.StringListEntityIdGetter ID_GETTER = StringListEntity_.__ID_GETTER;
                    
                        private final StringListConverter stringListConverter = new StringListConverter();
                    
                        private final static int __ID_stringList = StringListEntity_.stringList.id;
                    
                        public StringListEntityCursor(io.objectbox.Transaction tx, long cursor, BoxStore boxStore) {
                            super(tx, cursor, StringListEntity_.__INSTANCE, boxStore);
                        }
                    
                        @Override
                        public long getId(StringListEntity entity) {
                            return ID_GETTER.getId(entity);
                        }
                    
                        @SuppressWarnings({"rawtypes", "unchecked"}) 
                        @Override
                        public long put(StringListEntity entity) {
                            List stringList = entity.stringList;
                            int __id1 = stringList != null ? __ID_stringList : 0;
                    
                            long __assignedId = collectStringArray(cursor, entity.id, PUT_FLAG_FIRST | PUT_FLAG_COMPLETE,
                                    __id1, __id1 != 0 ? stringListConverter.convertToDatabaseValue(stringList) : null);
                    
                            entity.id = __assignedId;
                    
                            return __assignedId;
                        }
                    
                    }
                    """.trimIndent()
                generatedSourceFileMatches("com.example.StringListEntityCursor", expectedCursor)
            }
    }

}