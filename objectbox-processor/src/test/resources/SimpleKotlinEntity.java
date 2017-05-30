package io.objectbox.processor.test;

import io.objectbox.annotation.Entity;
import io.objectbox.annotation.Id;
import org.jetbrains.annotations.Nullable;

//
// Copy of decompiled byte-code of a Kotlin data class
//

@Entity
public final class SimpleKotlinEntity {
    @Id
    @Nullable
    private final Long _id;
    @Nullable
    private final Short simpleShort;
    @Nullable
    private final Integer simpleInt;
    @Nullable
    private final Long simpleLong;

    @Nullable
    public final Long get_id() {
        return this._id;
    }

    @Nullable
    public final Short getSimpleShort() {
        return this.simpleShort;
    }

    @Nullable
    public final Integer getSimpleInt() {
        return this.simpleInt;
    }

    @Nullable
    public final Long getSimpleLong() {
        return this.simpleLong;
    }

    public SimpleKotlinEntity(@Nullable Long _id, @Nullable Short simpleShort, @Nullable Integer simpleInt, @Nullable Long simpleLong) {
        this._id = _id;
        this.simpleShort = simpleShort;
        this.simpleInt = simpleInt;
        this.simpleLong = simpleLong;
    }

    @Nullable
    public final Long component1() {
        return this._id;
    }

    @Nullable
    public final Short component2() {
        return this.simpleShort;
    }

    @Nullable
    public final Integer component3() {
        return this.simpleInt;
    }

    @Nullable
    public final Long component4() {
        return this.simpleLong;
    }

    // copy and equals methods left out as they do not affect us
}