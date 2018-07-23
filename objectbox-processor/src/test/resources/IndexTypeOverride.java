package io.objectbox.processor.test;

import io.objectbox.annotation.Entity;
import io.objectbox.annotation.Id;
import io.objectbox.annotation.Index;
import io.objectbox.annotation.IndexType;

import java.util.Date;

@Entity
public class IndexTypeOverride {

    @Id long id;

    @Index(type = IndexType.VALUE) String valueProp;
    @Index(type = IndexType.HASH64) String hash64Prop;

}
