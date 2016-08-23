package org.greenrobot.greendao.example;

import org.greenrobot.greendao.annotation.Entity;
import org.greenrobot.greendao.annotation.Generated;
import org.greenrobot.greendao.annotation.Id;
import org.greenrobot.greendao.annotation.Transient;

@Entity
public class Note {

    @Id
    private Long id;

    // KEEP FIELDS - put your custom fields here
    @Transient
    private String text;
    // KEEP FIELDS END

    @Generated(hash = 1390446558)
    public Note(Long id) {
        this.id = id;
    }

    @Generated(hash = 1272611929)
    public Note() {
    }

    public Long getId() {
        return this.id;
    }

    public void setId(Long id) {
        this.id = id;
    }

}
