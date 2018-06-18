package io.objectbox.processor.test;

import java.util.List;

import io.objectbox.BoxStore;
import io.objectbox.annotation.Convert;
import io.objectbox.annotation.Entity;
import io.objectbox.annotation.Id;
import io.objectbox.converter.PropertyConverter;
import io.objectbox.relation.ToOne;

@Entity
public class ToOneAllArgs {

    @Id
    Long id;

    @Convert(converter = SimpleEnumConverter.class, dbType = String.class)
    SimpleEnum someString;

    @Convert(converter = SimpleListConverter.class, dbType = String.class)
    List<String> someArray;

    ToOne<ToOneParent> parent = new ToOne<>(this, ToOneAllArgs_.parent);

    // need to add manually, as processor can not modify entity
    transient BoxStore __boxStore;

    public ToOneAllArgs(Long id, SimpleEnum someString, List<String> someArray, long parentId) {
        this.id = id;
        this.someString = someString;
        this.someArray = someArray;
        this.parent.setTargetId(parentId);
    }

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

    public static class SimpleListConverter implements PropertyConverter<List<String>, String> {
        @Override
        public List<String> convertToEntityProperty(String databaseValue) {
            return null;
        }

        @Override
        public String convertToDatabaseValue(List<String> entityProperty) {
            return null;
        }
    }

}
