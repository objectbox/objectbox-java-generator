package io.objectbox.processor.test;

import io.objectbox.annotation.BaseEntity;

public abstract class InheritanceNoBase extends InheritanceBase {

    private String noBaseString;

    public String getNoBaseString() {
        return noBaseString;
    }

    public void setNoBaseString(String noBaseString) {
        this.noBaseString = noBaseString;
    }
}
