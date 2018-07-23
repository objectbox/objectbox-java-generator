package io.objectbox.processor.test;

import io.objectbox.annotation.Entity;
import io.objectbox.annotation.Id;
import io.objectbox.annotation.Index;
import io.objectbox.annotation.IndexType;

import java.util.Date;

@Entity
public class IndexTypeOverride {

    @Id long id;

    // all but byte[], float or double support @Index, compare with https://docs.objectbox.io/advanced/custom-types
    @Index(type = IndexType.HASH) Boolean boolPropOrNull;
    @Index(type = IndexType.HASH) boolean boolProp;

    @Index(type = IndexType.HASH) Integer intPropOrNull;
    @Index(type = IndexType.HASH) int intProp;

    @Index(type = IndexType.HASH) Long longPropOrNull;
    @Index(type = IndexType.HASH) long longProp;

    @Index(type = IndexType.HASH) Byte bytePropOrNull;
    @Index(type = IndexType.HASH) byte byteProp;

    @Index(type = IndexType.HASH) Character charPropOrNull;
    @Index(type = IndexType.HASH) char charProp;

    @Index(type = IndexType.VALUE) String stringProp;

    @Index(type = IndexType.HASH) Date dateProp;

}
