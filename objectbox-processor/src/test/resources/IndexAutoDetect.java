package io.objectbox.processor.test;

import io.objectbox.annotation.Entity;
import io.objectbox.annotation.Id;
import io.objectbox.annotation.Index;
import java.util.Date;

@Entity
public class IndexAutoDetect {

    @Id long id;

    // all but byte[], float or double support @Index, compare with https://docs.objectbox.io/advanced/custom-types
    @Index Boolean boolPropOrNull;
    @Index boolean boolProp;

    @Index Integer intPropOrNull;
    @Index int intProp;

    @Index Long longPropOrNull;
    @Index long longProp;

    @Index Byte bytePropOrNull;
    @Index byte byteProp;

    @Index Character charPropOrNull;
    @Index char charProp;

    @Index String stringProp;

    @Index Date dateProp;

}
