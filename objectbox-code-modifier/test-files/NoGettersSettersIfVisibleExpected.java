package io.objectbox.codemodifier.test;

import io.objectbox.annotation.Entity;
import io.objectbox.annotation.Id;
import io.objectbox.annotation.Generated;

@Entity
public class Note {

    @Id
    Long id;

    private String getterSetter;

    String noGetterSetter;

    protected String noGetterSetter2;

    public String noGetterSetter3;

    @Generated(hash = 813810191)
    public Note(Long id, String getterSetter, String noGetterSetter, String noGetterSetter2, String noGetterSetter3) {
        this.id = id;
        this.getterSetter = getterSetter;
        this.noGetterSetter = noGetterSetter;
        this.noGetterSetter2 = noGetterSetter2;
        this.noGetterSetter3 = noGetterSetter3;
    }

    @Generated(hash = 1272611929)
    public Note() {
    }

    public String getGetterSetter() {
        return getterSetter;
    }

    public void setGetterSetter(String getterSetter) {
        this.getterSetter = getterSetter;
    }

}
