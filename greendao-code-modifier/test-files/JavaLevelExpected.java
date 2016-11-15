package org.greenrobot.greendao.example;

import org.greenrobot.greendao.annotation.Entity;
import org.greenrobot.greendao.annotation.Id;
import org.greenrobot.greendao.annotation.Generated;

@Entity
public class Note {

    @Id
    private Long id;

    @Generated(hash = 1390446558)
    public Note(Long id) {
        this.id = id;
    }

    @Generated(hash = 1272611929)
    public Note() {
    }

    /** Switch on string: ensure that parser source + compliance level is at least 1.7. */
    public void doSwitch(String sandra) {
        switch (sandra) {
            default:
                break;
        }
    }

    public Long getId() {
        return this.id;
    }

    public void setId(Long id) {
        this.id = id;
    }

}
