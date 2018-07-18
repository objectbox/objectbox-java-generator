package io.objectbox.processor.test;

import io.objectbox.annotation.Entity;
import io.objectbox.annotation.Id;
import io.objectbox.annotation.Index;
import io.objectbox.annotation.IndexType;

import java.util.Date;

@Entity
public class IndexMaxLengthFailType {

    @Id long id;

    // index type wrong, prop type correct
    @Index(type = IndexType.HASH, maxValueLength = 42) String strinProp;
}
