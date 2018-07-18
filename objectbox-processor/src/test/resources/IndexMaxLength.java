package io.objectbox.processor.test;

import io.objectbox.annotation.Entity;
import io.objectbox.annotation.Id;
import io.objectbox.annotation.Index;
import io.objectbox.annotation.IndexType;

import java.util.Date;

@Entity
public class IndexMaxLength {

    @Id long id;

    @Index(type = IndexType.VALUE) String stringProp; // default 0 (ensured by compiler)
    @Index(type = IndexType.VALUE, maxValueLength = 42) byte[] byteArrayProp;
}
