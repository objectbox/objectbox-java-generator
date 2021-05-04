package io.objectbox.processor.test;

import io.objectbox.annotation.ConflictStrategy;
import io.objectbox.annotation.Entity;
import io.objectbox.annotation.Id;
import io.objectbox.annotation.Index;
import io.objectbox.annotation.IndexType;
import io.objectbox.annotation.Unique;

import java.util.Date;

@Entity
public class UniqueGenerated {

    @Id long id;

    @Unique
    int intProp;

    @Unique
    @Index(type = IndexType.VALUE)
    String stringProp;

    @Unique(onConflict = ConflictStrategy.REPLACE)
    long replaceProp;

}
