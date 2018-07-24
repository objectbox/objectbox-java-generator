package io.objectbox.processor.test;

import io.objectbox.annotation.Entity;
import io.objectbox.annotation.Id;
import io.objectbox.annotation.Index;
import io.objectbox.relation.ToOne;
import io.objectbox.relation.ToMany;

import java.util.Date;
import java.util.List;

@Entity
public class IndexUnsupported {

    @Id @Index long id;

    @Index ToOne<IndexUnsupported> toOne;
    @Index ToMany<IndexUnsupported> toMany;
    @Index List<IndexUnsupported> toManyList;

    // byte[], float or double do not support @Index
    @Index Float floatPropOrNull;
    @Index float floatProp;

    @Index Double doublePropOrNull;
    @Index double doubleProp;

    @Index byte[] byteArrayProp;

}
