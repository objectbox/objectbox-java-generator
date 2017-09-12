package io.objectbox.test.entityannotation;

import io.objectbox.annotation.Entity;
import io.objectbox.annotation.Id;

@Entity
public class NotNullThing {

    @Id
    private Long id;

    boolean nullableBoolean;
    int nullableInteger;
    Boolean nullableWrappedBoolean;
    Integer nullableWrappedInteger;

    boolean notNullBoolean;
    int notNullInteger;
    Boolean notNullWrappedBoolean;
    Integer notNullWrappedInteger;

    public NotNullThing(Long id, boolean nullableBoolean, int nullableInteger,
            Boolean nullableWrappedBoolean, Integer nullableWrappedInteger, boolean notNullBoolean,
            int notNullInteger, Boolean notNullWrappedBoolean, Integer notNullWrappedInteger) {
        this.id = id;
        this.nullableBoolean = nullableBoolean;
        this.nullableInteger = nullableInteger;
        this.nullableWrappedBoolean = nullableWrappedBoolean;
        this.nullableWrappedInteger = nullableWrappedInteger;
        this.notNullBoolean = notNullBoolean;
        this.notNullInteger = notNullInteger;
        this.notNullWrappedBoolean = notNullWrappedBoolean;
        this.notNullWrappedInteger = notNullWrappedInteger;
    }

    public NotNullThing() {
    }

    public Long getId() {
        return this.id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public boolean getNullableBoolean() {
        return this.nullableBoolean;
    }

    public void setNullableBoolean(boolean nullableBoolean) {
        this.nullableBoolean = nullableBoolean;
    }

    public int getNullableInteger() {
        return this.nullableInteger;
    }

    public void setNullableInteger(int nullableInteger) {
        this.nullableInteger = nullableInteger;
    }

    public Boolean getNullableWrappedBoolean() {
        return this.nullableWrappedBoolean;
    }

    public void setNullableWrappedBoolean(Boolean nullableWrappedBoolean) {
        this.nullableWrappedBoolean = nullableWrappedBoolean;
    }

    public Integer getNullableWrappedInteger() {
        return this.nullableWrappedInteger;
    }

    public void setNullableWrappedInteger(Integer nullableWrappedInteger) {
        this.nullableWrappedInteger = nullableWrappedInteger;
    }

    public boolean getNotNullBoolean() {
        return this.notNullBoolean;
    }

    public void setNotNullBoolean(boolean notNullBoolean) {
        this.notNullBoolean = notNullBoolean;
    }

    public int getNotNullInteger() {
        return this.notNullInteger;
    }

    public void setNotNullInteger(int notNullInteger) {
        this.notNullInteger = notNullInteger;
    }

    public Boolean getNotNullWrappedBoolean() {
        return this.notNullWrappedBoolean;
    }

    public void setNotNullWrappedBoolean(Boolean notNullWrappedBoolean) {
        this.notNullWrappedBoolean = notNullWrappedBoolean;
    }

    public Integer getNotNullWrappedInteger() {
        return this.notNullWrappedInteger;
    }

    public void setNotNullWrappedInteger(Integer notNullWrappedInteger) {
        this.notNullWrappedInteger = notNullWrappedInteger;
    }

}
