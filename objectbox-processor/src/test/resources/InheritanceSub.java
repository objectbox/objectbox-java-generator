package io.objectbox.processor.test;

import io.objectbox.annotation.Entity;

@Entity
public class InheritanceSub extends InheritanceNoBase implements InheritanceInterface {

    private String subString;

    public String getSubString() {
        return subString;
    }

    public void setSubString(String subString) {
        this.subString = subString;
    }

}
