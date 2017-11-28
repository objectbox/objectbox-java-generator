package io.objectbox.processor.test;

import io.objectbox.annotation.Entity;

@Entity
public class InheritanceSubSub extends InheritanceSub {

    private String subSubString;

    public String getSubSubString() {
        return subSubString;
    }

    public void setSubSubString(String subSubString) {
        this.subSubString = subSubString;
    }

}
