package io.objectbox.processor.test;

import io.objectbox.annotation.Entity;
import io.objectbox.annotation.Id;
import io.objectbox.annotation.Index;
import java.util.Date;

@Entity
public class IndexUnsupported {

    @Id long id;

    // byte[], float or double do not support @Index
    @Index Float floatPropOrNull;
    @Index float floatProp;

    @Index Double doublePropOrNull;
    @Index double doubleProp;

    @Index byte[] byteArrayProp;

}
