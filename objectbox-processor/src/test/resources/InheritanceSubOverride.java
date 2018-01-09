package io.objectbox.processor.test;

import io.objectbox.annotation.Entity;

@Entity
public class InheritanceSubOverride extends InheritanceBase {

    private String subString;

    String overriddenString;

    public String getSubString() {
        return subString;
    }

    public void setSubString(String subString) {
        this.subString = subString;
    }

}
