package io.objectbox.processor.test;

import io.objectbox.annotation.Entity;
import io.objectbox.annotation.Id;
import io.objectbox.annotation.Transient;

import java.util.Date;

@Entity
public class SimpleEntity {

    @Id
    long id;

    short simpleShortPrimitive;
    Short simpleShort;

    int simpleIntPrimitive;
    Integer simpleInt;

    long simpleLongPrimitive;
    Long simepleLong;

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
}
