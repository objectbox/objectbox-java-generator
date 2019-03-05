package io.objectbox.processor.test;

import io.objectbox.annotation.BaseEntity;
import io.objectbox.annotation.Id;

@BaseEntity
public abstract class InheritanceBaseGeneric<T> {

    private String baseString;

    public String getBaseString() {
        return baseString;
    }

    public void setBaseString(String baseString) {
        this.baseString = baseString;
    }
}
