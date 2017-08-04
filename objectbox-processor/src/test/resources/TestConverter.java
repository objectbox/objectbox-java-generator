package io.objectbox.processor.test;

import io.objectbox.annotation.Entity;
import io.objectbox.annotation.NameInDb;
import io.objectbox.converter.PropertyConverter;

public class TestConverter implements PropertyConverter<String, String> {
    @Override
    public String convertToEntityProperty(String databaseValue) {
        return databaseValue == null ? null : databaseValue.substring(0, databaseValue.length() - 1);
    }

    @Override
    public String convertToDatabaseValue(String entityProperty) {
        return entityProperty == null ? null : entityProperty + "!";
    }
}
