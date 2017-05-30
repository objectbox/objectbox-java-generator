package io.objectbox.processor.test;

import java.util.Date;

import io.objectbox.annotation.Convert;
import io.objectbox.annotation.Entity;
import io.objectbox.annotation.Id;
import io.objectbox.annotation.Index;
import io.objectbox.annotation.NameInDb;
import io.objectbox.annotation.Transient;
import io.objectbox.annotation.Uid;
import io.objectbox.converter.PropertyConverter;

@Entity
@NameInDb("A")
@Uid(4858050548069557694L)
public class SimpleEntity {

    @Id(assignable = true)
    long id;

    short simpleShortPrimitive;
    Short simpleShort;

    int simpleIntPrimitive;
    Integer simpleInt;

    long simpleLongPrimitive;
    Long simpleLong;

    float simpleFloatPrimitive;
    Float simpleFloat;

    double simpleDoublePrimitive;
    Double simpleDouble;

    boolean simpleBooleanPrimitive;
    Boolean simpleBoolean;

    byte simpleBytePrimitive;
    Byte simpleByte;

    Date simpleDate;
    String simpleString;

    byte[] simpleByteArray;

    static String transientField;
    transient String transientField2;
    @Transient
    String transientField3;

    @Index
    Integer indexedProperty;

    @NameInDb("B")
    String namedProperty;

    @Uid(3817914863709111804L)
    Long uidProperty;

    @Convert(converter = SimpleEnumConverter.class, dbType = Integer.class)
    SimpleEnum customType;

    public enum SimpleEnum {
        DEFAULT(0), A(1), B(2);

        final int id;

        SimpleEnum(int id) {
            this.id = id;
        }
    }

    public static class SimpleEnumConverter implements PropertyConverter<SimpleEnum, Integer> {
        @Override
        public SimpleEnum convertToEntityProperty(Integer databaseValue) {
            if (databaseValue == null) {
                return null;
            }
            for (SimpleEnum value : SimpleEnum.values()) {
                if (value.id == databaseValue) {
                    return value;
                }
            }
            return SimpleEnum.DEFAULT;
        }

        @Override
        public Integer convertToDatabaseValue(SimpleEnum entityProperty) {
            return entityProperty == null ? null : entityProperty.id;
        }
    }
}