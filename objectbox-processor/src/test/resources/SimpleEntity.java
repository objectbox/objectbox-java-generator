package io.objectbox.processor.test;

import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;

import io.objectbox.annotation.Convert;
import io.objectbox.annotation.DatabaseType;
import io.objectbox.annotation.Entity;
import io.objectbox.annotation.HnswFlags;
import io.objectbox.annotation.HnswIndex;
import io.objectbox.annotation.Id;
import io.objectbox.annotation.IdCompanion;
import io.objectbox.annotation.Index;
import io.objectbox.annotation.NameInDb;
import io.objectbox.annotation.Transient;
import io.objectbox.annotation.Type;
import io.objectbox.annotation.Uid;
import io.objectbox.annotation.VectorDistanceType;
import io.objectbox.converter.PropertyConverter;
import io.objectbox.relation.ToMany;
import io.objectbox.relation.ToOne;

@Entity
@NameInDb("A")
public class SimpleEntity {

    @Id(assignable = true)
    long id;

    short simpleShortPrimitive;
    Short simpleShort;

    int simpleIntPrimitive;
    Integer simpleInt;

    long simpleLongPrimitive;
    Long simpleLong;

    float simpleFloatPrimitive;
    Float simpleFloat;

    double simpleDoublePrimitive;
    Double simpleDouble;

    private boolean simpleBooleanPrimitive;
    private Boolean simpleBoolean;

    byte simpleBytePrimitive;
    Byte simpleByte;

    Date simpleDate;

    char simpleCharPrimitive;
    Character simpleChar;

    String simpleString;

    byte[] simpleByteArray;

    String[] simpleStringArray;
    List<String> simpleStringList;

    static String transientField;
    transient String transientField2;
    @Transient
    String transientField3;

    @Index
    Integer indexedProperty;

    @NameInDb("B")
    String namedProperty;

    @Convert(converter = SimpleEnumConverter.class, dbType = Integer.class)
    SimpleEnum customType;

    @Convert(converter = SimpleEnumListConverter.class, dbType = Integer.class)
    List<SimpleEnum> customTypes;

    @Type(DatabaseType.DateNano)
    long dateNanoPrimitive;
    @Type(DatabaseType.DateNano)
    Long dateNano;

    @IdCompanion
    Date idCompanion;

    ToOne<IdEntity> toOne = new ToOne<>(this, SimpleEntity_.toOne);

    ToMany<IdEntity> toMany = new ToMany<>(this, SimpleEntity_.toMany);

    Map<String, Object> stringFlexMap;
    Object flexProperty;

    boolean[] booleanArray;
    short[] shortArray;
    char[] charArray;
    int[] intArray;
    long[] longArray;
    float[] floatArray;
    double[] doubleArray;

    @HnswIndex(
            dimensions = 2,
            neighborsPerNode = 30,
            indexingSearchCount = 100,
            flags = @HnswFlags(
                    debugLogs = true,
                    debugLogsDetailed = true,
                    vectorCacheSimdPaddingOff = true,
                    reparationLimitCandidates = true),
            distanceType = VectorDistanceType.EUCLIDEAN,
            reparationBacklinkProbability = 0.95F,
            vectorCacheHintSizeKB = 2097152
    )
    float[] floatArrayHnsw;

    public boolean isSimpleBooleanPrimitive() {
        return simpleBooleanPrimitive;
    }

    public void setSimpleBooleanPrimitive(boolean simpleBooleanPrimitive) {
        this.simpleBooleanPrimitive = simpleBooleanPrimitive;
    }

    public Boolean getSimpleBoolean() {
        return simpleBoolean;
    }

    public void setSimpleBoolean(Boolean simpleBoolean) {
        this.simpleBoolean = simpleBoolean;
    }

    public enum SimpleEnum {
        DEFAULT(0), A(1), B(2);

        final int id;

        SimpleEnum(int id) {
            this.id = id;
        }
    }

    public static class SimpleEnumConverter implements PropertyConverter<SimpleEnum, Integer> {
        @Override
        public SimpleEnum convertToEntityProperty(Integer databaseValue) {
            return SimpleEnum.DEFAULT;
        }

        @Override
        public Integer convertToDatabaseValue(SimpleEnum entityProperty) {
            return SimpleEnum.DEFAULT.id;
        }
    }

    public static class SimpleEnumListConverter implements PropertyConverter<List<SimpleEnum>, Integer> {
        @Override
        public List<SimpleEnum> convertToEntityProperty(Integer databaseValue) {
            return Collections.singletonList(SimpleEnum.DEFAULT);
        }

        @Override
        public Integer convertToDatabaseValue(List<SimpleEnum> entityProperty) {
            return SimpleEnum.DEFAULT.id;
        }
    }
}
