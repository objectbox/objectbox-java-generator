package io.objectbox.processor.test;

import io.objectbox.annotation.Entity;
import io.objectbox.annotation.Id;
import org.jetbrains.annotations.Nullable;

//
// Copy of decompiled byte-code of a Kotlin data class, modified to remove parameter names
//

@Entity
public final class SimpleKotlinEntity {
    @Id
    private long id;
    @Nullable
    private final Short simpleShort;
    @Nullable
    private final Integer simpleInt;
    @Nullable
    private final Long simpleLong;

    public final long getId() {
        return this.id;
    }

    public final void setId(long var1) {
        this.id = var1;
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

    public SimpleKotlinEntity(long arg0, @Nullable Short arg1, @Nullable Integer arg2, @Nullable Long arg3) {
        this.id = arg0;
        this.simpleShort = arg1;
        this.simpleInt = arg2;
        this.simpleLong = arg3;
    }

    // methods left out as they do not affect us: copy, equals, hashCode, component1..N
}