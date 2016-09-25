package org.greenrobot.greendao.example;

import io.objectbox.annotation.Entity;
import io.objectbox.annotation.Id;
import java.lang.Boolean;
import java.lang.Byte;
import java.lang.Integer;
import java.lang.Long;
import java.lang.Float;
import java.lang.Double;
import java.lang.Short;

@Entity
public class Note {

    @Id
    private Long id;

    private Boolean booleanProperty;
    private Byte byteProperty;
    private Integer integerProperty;
    private Float floatProperty;
    private Double doubleProperty;
    private Short shortProperty;

}
