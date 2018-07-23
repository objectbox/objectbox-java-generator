package io.objectbox.processor.test;

import io.objectbox.annotation.Entity;
import io.objectbox.annotation.Id;
import io.objectbox.annotation.Index;
import io.objectbox.annotation.IndexType;

import java.util.Date;

@Entity
public class IndexHash64NotSupported {

    @Id long id;

    // only String supports HASH64, compare with https://docs.objectbox.io/advanced/custom-types
    @Index(type = IndexType.HASH64) Boolean boolPropOrNull;
    @Index(type = IndexType.HASH64) boolean boolProp;

    @Index(type = IndexType.HASH64) Integer intPropOrNull;
    @Index(type = IndexType.HASH64) int intProp;

    @Index(type = IndexType.HASH64) Long longPropOrNull;
    @Index(type = IndexType.HASH64) long longProp;

    @Index(type = IndexType.HASH64) Byte bytePropOrNull;
    @Index(type = IndexType.HASH64) byte byteProp;

    @Index(type = IndexType.HASH64) Character charPropOrNull;
    @Index(type = IndexType.HASH64) char charProp;

    @Index(type = IndexType.HASH64) Date dateProp;

}
