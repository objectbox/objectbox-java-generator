package io.objectbox.processor.test;

import io.objectbox.annotation.Entity;
import io.objectbox.annotation.Id;
import io.objectbox.annotation.Unique;
import io.objectbox.relation.ToMany;
import io.objectbox.relation.ToOne;

import java.util.Date;
import java.util.List;

@Entity
public class UniqueUnsupported {

    @Id @Unique long id;

    @Unique ToOne<UniqueUnsupported> toOne;
    @Unique ToMany<UniqueUnsupported> toMany;
    @Unique List<UniqueUnsupported> toManyList;

    // byte[], float or double do not support @Unique
    @Unique Float floatPropOrNull;
    @Unique float floatProp;

    @Unique Double doublePropOrNull;
    @Unique double doubleProp;

    @Unique byte[] byteArrayProp;

}
