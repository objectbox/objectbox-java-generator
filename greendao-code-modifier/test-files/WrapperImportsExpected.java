package org.greenrobot.greendao.example;

import org.greenrobot.greendao.annotation.Entity;
import org.greenrobot.greendao.annotation.Id;
import java.lang.Boolean;
import java.lang.Byte;
import java.lang.Integer;
import java.lang.Long;
import java.lang.Float;
import java.lang.Double;
import java.lang.Short;
import org.greenrobot.greendao.annotation.Generated;

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
    public Short getShortProperty() {
        return this.shortProperty;
    }
    public void setShortProperty(Short shortProperty) {
        this.shortProperty = shortProperty;
    }
    public Double getDoubleProperty() {
        return this.doubleProperty;
    }
    public void setDoubleProperty(Double doubleProperty) {
        this.doubleProperty = doubleProperty;
    }
    public Float getFloatProperty() {
        return this.floatProperty;
    }
    public void setFloatProperty(Float floatProperty) {
        this.floatProperty = floatProperty;
    }
    public Integer getIntegerProperty() {
        return this.integerProperty;
    }
    public void setIntegerProperty(Integer integerProperty) {
        this.integerProperty = integerProperty;
    }
    public Byte getByteProperty() {
        return this.byteProperty;
    }
    public void setByteProperty(Byte byteProperty) {
        this.byteProperty = byteProperty;
    }
    public Boolean getBooleanProperty() {
        return this.booleanProperty;
    }
    public void setBooleanProperty(Boolean booleanProperty) {
        this.booleanProperty = booleanProperty;
    }
    public Long getId() {
        return this.id;
    }
    public void setId(Long id) {
        this.id = id;
    }
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

}
