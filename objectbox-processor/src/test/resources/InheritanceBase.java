package io.objectbox.processor.test;

import io.objectbox.annotation.Id;

public abstract class InheritanceBase {

    @Id
    private long id;

    private String baseString;

    String overriddenString;

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getBaseString() {
        return baseString;
    }

    public void setBaseString(String baseString) {
        this.baseString = baseString;
    }
}
