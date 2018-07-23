package io.objectbox.processor.test;

import io.objectbox.annotation.Entity;
import io.objectbox.annotation.Id;
import io.objectbox.annotation.Unique;
import java.util.Date;

@Entity
public class UniqueUnsupported {

    @Id long id;

    // byte[], float or double do not support @Unique
    @Unique Float floatPropOrNull;
    @Unique float floatProp;

    @Unique Double doublePropOrNull;
    @Unique double doubleProp;

    @Unique byte[] byteArrayProp;

}
