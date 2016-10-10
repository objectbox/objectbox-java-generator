package org.greenrobot.greendao.example;

import io.objectbox.annotation.Convert;
import io.objectbox.annotation.Entity;
import io.objectbox.annotation.Id;
import org.greenrobot.greendao.converter.PropertyConverter;

@Entity
public class Note {

    @Id
    private Long id;

    @Convert(converter = NoteTypeConverter.class, dbType = String.class)
    private NoteType type;

    enum NoteType {
        TEXT, LIST, PICTURE
    }

    static class NoteTypeConverter implements PropertyConverter<NoteType, String> {
        @Override
        public NoteType convertToEntityProperty(String databaseValue) {
            return NoteType.valueOf(databaseValue);
        }

        @Override
        public String convertToDatabaseValue(NoteType entityProperty) {
            return entityProperty.name();
        }
    }

}
