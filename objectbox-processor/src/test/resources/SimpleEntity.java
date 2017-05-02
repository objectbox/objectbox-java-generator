package io.objectbox.processor.test;

import io.objectbox.annotation.Entity;
import io.objectbox.annotation.Id;

import java.util.Date;

@Entity
public class SimpleEntity {

    @Id
    private long id;

    private String text;
    private String comment;
    private Date date;

}
