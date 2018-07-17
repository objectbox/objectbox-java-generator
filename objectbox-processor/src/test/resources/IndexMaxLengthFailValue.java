package io.objectbox.processor.test;

import io.objectbox.annotation.Entity;
import io.objectbox.annotation.Id;
import io.objectbox.annotation.Index;
import io.objectbox.annotation.IndexType;

import java.util.Date;

@Entity
public class IndexMaxLengthFailValue {

    @Id long id;

    // index type correct, prop type correct, but value not allowed
    @Index(type = IndexType.VALUE, maxValueLength = -42) String intProp;
}
