package io.objectbox.processor.test;

import io.objectbox.annotation.Convert;
import io.objectbox.annotation.Entity;
import io.objectbox.annotation.Id;
import io.objectbox.relation.ToMany;

@Entity
public class ToManyAndConverter {

    @Id
    Long id;

    @Convert(converter = TestConverter.class, dbType = String.class)
    String convertedString;

    ToMany<IdEntity> children = new ToMany<>(this, ToManyAndConverter_.children);

}
