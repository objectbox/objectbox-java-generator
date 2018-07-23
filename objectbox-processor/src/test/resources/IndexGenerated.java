package io.objectbox.processor.test;

import io.objectbox.annotation.Entity;
import io.objectbox.annotation.Id;
import io.objectbox.annotation.Index;
import io.objectbox.annotation.IndexType;

import java.util.Date;

@Entity
public class IndexGenerated {

    @Id long id;

    // all possible values for 'type'
    @Index int defaultProp; // DEFAULT (ensured by compiler)
    @Index(type = IndexType.VALUE) boolean valueProp;
    @Index(type = IndexType.HASH) String hashProp;
    @Index(type = IndexType.HASH64) String hash64Prop;

}
