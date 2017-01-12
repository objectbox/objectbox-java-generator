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
import io.objectbox.annotation.Generated;

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
    @Generated(hash = 1322397276)
    public Note(Long id, Boolean booleanProperty, Byte byteProperty, Integer integerProperty, Float floatProperty,
            Double doubleProperty, Short shortProperty) {
        this.id = id;
        this.booleanProperty = booleanProperty;
        this.byteProperty = byteProperty;
        this.integerProperty = integerProperty;
        this.floatProperty = floatProperty;
        this.doubleProperty = doubleProperty;
        this.shortProperty = shortProperty;
    }
    @Generated(hash = 1272611929)
    public Note() {
    }
    public Long getId() {
        return id;
    }
    public void setId(Long id) {
        this.id = id;
    }
    public Boolean getBooleanProperty() {
        return booleanProperty;
    }
    public void setBooleanProperty(Boolean booleanProperty) {
        this.booleanProperty = booleanProperty;
    }
    public Byte getByteProperty() {
        return byteProperty;
    }
    public void setByteProperty(Byte byteProperty) {
        this.byteProperty = byteProperty;
    }
    public Integer getIntegerProperty() {
        return integerProperty;
    }
    public void setIntegerProperty(Integer integerProperty) {
        this.integerProperty = integerProperty;
    }
    public Float getFloatProperty() {
        return floatProperty;
    }
    public void setFloatProperty(Float floatProperty) {
        this.floatProperty = floatProperty;
    }
    public Double getDoubleProperty() {
        return doubleProperty;
    }
    public void setDoubleProperty(Double doubleProperty) {
        this.doubleProperty = doubleProperty;
    }
    public Short getShortProperty() {
        return shortProperty;
    }
    public void setShortProperty(Short shortProperty) {
        this.shortProperty = shortProperty;
    }

}
