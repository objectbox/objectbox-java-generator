package io.objectbox.processor.test;

import io.objectbox.annotation.Convert;
import io.objectbox.annotation.Entity;
import io.objectbox.annotation.Id;
import io.objectbox.annotation.Index;
import io.objectbox.annotation.NameInDb;
import io.objectbox.converter.PropertyConverter;
import io.objectbox.annotation.Uid;

@Entity
public class MultipleEntity {
    @Id
    Long id;

    @Convert(converter = SimpleEnumConverter.class, dbType = String.class)
    @NameInDb("A")
    @Index
    @Uid(167962951075785953L)
    SimpleEnum someString;

    public enum SimpleEnum {DEFAULT}

    public static class SimpleEnumConverter implements PropertyConverter<SimpleEnum, String> {
        @Override
        public SimpleEnum convertToEntityProperty(String databaseValue) {
            return SimpleEnum.DEFAULT;
        }

        @Override
        public String convertToDatabaseValue(SimpleEnum entityProperty) {
            return "";
        }
    }
}