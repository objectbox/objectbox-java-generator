package io.objectbox.processor.test;

import io.objectbox.annotation.Entity;
import io.objectbox.annotation.Id;
import io.objectbox.annotation.Index;
import java.util.Date;

@Entity
public class IndexAutoDetect {

    @Id long id;

    // all supported types, compare with https://docs.objectbox.io/advanced/custom-types
    @Index Boolean boolPropOrNull;
    @Index boolean boolProp;

    @Index Integer intPropOrNull;
    @Index int intProp;

    @Index Long longPropOrNull;
    @Index long longProp;

    @Index Float floatPropOrNull;
    @Index float floatProp;

    @Index Double doublePropOrNull;
    @Index double doubleProp;

    @Index Byte bytePropOrNull;
    @Index byte byteProp;

    @Index Character charPropOrNull;
    @Index char charProp;

    @Index byte[] byteArrayProp;

    @Index String stringProp;

    @Index Date dateProp;

}
