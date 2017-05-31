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

    public SimpleKotlinEntity(long id, @Nullable Short simpleShort, @Nullable Integer simpleInt, @Nullable Long simpleLong) {
        this.id = id;
        this.simpleShort = simpleShort;
        this.simpleInt = simpleInt;
        this.simpleLong = simpleLong;
    }

    // methods left out as they do not affect us: copy, equals, hashCode, component1..N
}