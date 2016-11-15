package org.greenrobot.greendao.example;

import org.greenrobot.greendao.annotation.Entity;
import org.greenrobot.greendao.annotation.Id;

@Entity
public class Note {

    @Id
    private Long id;

    /** Switch on string: ensure that parser source + compliance level is at least 1.7. */
    public void doSwitch(String sandra) {
        switch (sandra) {
            default:
                break;
        }
    }

}
