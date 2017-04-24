package io.objectbox.codemodifier.test;

import io.objectbox.annotation.Entity;
import io.objectbox.annotation.Generated;
import io.objectbox.annotation.Id;
import io.objectbox.annotation.Property;
import io.objectbox.annotation.Uid;

@Entity
@Uid(3030961966062954432L)
public class Note {

    @Id
    private Long id;

    @Uid(3564132052700598090L)
    private String insert;

    @Uid(1406015203155591783L)
    private String doNotInsert;

    @Uid(6127924131172114129L)
    private String generateNew;

    private String control;

    @Generated(hash = 1366117147)
    public Note(Long id, String insert, String doNotInsert, String generateNew, String control) {
        this.id = id;
        this.insert = insert;
        this.doNotInsert = doNotInsert;
        this.generateNew = generateNew;
        this.control = control;
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

    public String getInsert() {
        return insert;
    }

    public void setInsert(String insert) {
        this.insert = insert;
    }

    public String getDoNotInsert() {
        return doNotInsert;
    }

    public void setDoNotInsert(String doNotInsert) {
        this.doNotInsert = doNotInsert;
    }

    public String getGenerateNew() {
        return generateNew;
    }

    public void setGenerateNew(String generateNew) {
        this.generateNew = generateNew;
    }

    public String getControl() {
        return control;
    }

    public void setControl(String control) {
        this.control = control;
    }

}
