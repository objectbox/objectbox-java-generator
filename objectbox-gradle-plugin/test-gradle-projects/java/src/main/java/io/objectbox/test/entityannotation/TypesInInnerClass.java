package io.objectbox.test.entityannotation;

import io.objectbox.annotation.Convert;
import io.objectbox.annotation.Entity;
import io.objectbox.annotation.Id;
import io.objectbox.annotation.Transient;
import io.objectbox.converter.PropertyConverter;

@Entity
public class TypesInInnerClass {
    static class MyInnerType {

        public MyInnerType(String value) {
            this.value = value;
        }

        String value;
    }

    static class MyInnerTypeConverter implements PropertyConverter<MyInnerType, Long> {

        @Override
        public MyInnerType convertToEntityProperty(Long databaseValue) {
            return databaseValue != null ? new MyInnerType(Long.toHexString(databaseValue)) : null;
        }

        @Override
        public Long convertToDatabaseValue(MyInnerType entityProperty) {
            return entityProperty != null ? Long.parseLong(entityProperty.value, 16) : null;
        }
    }

    @Id
    Long id;

    @Convert(converter = MyInnerTypeConverter.class, dbType = Long.class)
    TypesInInnerClass.MyInnerType type;

    String dummy;

    public MyInnerType getType() {
        return this.type;
    }

    public void setType(MyInnerType type) {
        this.type = type;
    }

    public Long getId() {
        return this.id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getDummy() {
        return this.dummy;
    }

    public void setDummy(String dummy) {
        this.dummy = dummy;
    }

    public TypesInInnerClass(Long id, TypesInInnerClass.MyInnerType type, String dummy) {
        this.id = id;
        this.type = type;
        this.dummy = dummy;
    }

    public TypesInInnerClass() {
    }

}
