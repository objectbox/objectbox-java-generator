package io.objectbox.processor.test;

import io.objectbox.annotation.Entity;
import io.objectbox.annotation.Id;
import io.objectbox.annotation.Index;
import io.objectbox.annotation.IndexType;

import java.util.Date;

@Entity
public class IndexMaxLengthFailProp {

    @Id long id;

    // index type correct, prop type wrong
    @Index(type = IndexType.VALUE, maxValueLength = 42) int intProp;
}
