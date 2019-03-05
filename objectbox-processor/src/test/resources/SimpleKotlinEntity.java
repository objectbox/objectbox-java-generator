package io.objectbox.processor.test;

import io.objectbox.annotation.Entity;
import io.objectbox.annotation.Id;
import org.jetbrains.annotations.Nullable;

import java.util.Date;

//
// Copy of decompiled byte-code of the below Kotlin data class, modified to remove parameter names (arg0, ...)
//
//data class SimpleKotlinEntity(
//        @Id
//        var id: Long = 0,
//
//        var simpleShort: Short? = null,
//
//        var simpleInt: Int? = null,
//
//        var simpleLong: Long? = null,
//
//        var simpleFloat: Float? = null,
//
//        var simpleDouble: Double? = null,
//
//        var simpleBoolean: Boolean? = null,
//
//        var simpleByte: Byte? = null,
//
//        var simpleDate: Date? = null,
//
//        var simpleString: String? = null,
//
//        var simpleByteArray: ByteArray? = null,
//
//        var isAnything: String? = null
//
//)

@Entity
public final class SimpleKotlinEntity {
    @Id
    private long id;
    @Nullable
    private Short simpleShort;
    @Nullable
    private Integer simpleInt;
    @Nullable
    private Long simpleLong;
    @Nullable
    private Float simpleFloat;
    @Nullable
    private Double simpleDouble;
    @Nullable
    private Boolean simpleBoolean;
    @Nullable
    private Byte simpleByte;
    @Nullable
    private Date simpleDate;
    @Nullable
    private String simpleString;
    @Nullable
    private byte[] simpleByteArray;
    @Nullable
    private String isAnything;

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

    public final void setSimpleShort(@Nullable Short var1) {
        this.simpleShort = var1;
    }

    @Nullable
    public final Integer getSimpleInt() {
        return this.simpleInt;
    }

    public final void setSimpleInt(@Nullable Integer var1) {
        this.simpleInt = var1;
    }

    @Nullable
    public final Long getSimpleLong() {
        return this.simpleLong;
    }

    public final void setSimpleLong(@Nullable Long var1) {
        this.simpleLong = var1;
    }

    @Nullable
    public final Float getSimpleFloat() {
        return this.simpleFloat;
    }

    public final void setSimpleFloat(@Nullable Float var1) {
        this.simpleFloat = var1;
    }

    @Nullable
    public final Double getSimpleDouble() {
        return this.simpleDouble;
    }

    public final void setSimpleDouble(@Nullable Double var1) {
        this.simpleDouble = var1;
    }

    @Nullable
    public final Boolean getSimpleBoolean() {
        return this.simpleBoolean;
    }

    public final void setSimpleBoolean(@Nullable Boolean var1) {
        this.simpleBoolean = var1;
    }

    @Nullable
    public final Byte getSimpleByte() {
        return this.simpleByte;
    }

    public final void setSimpleByte(@Nullable Byte var1) {
        this.simpleByte = var1;
    }

    @Nullable
    public final Date getSimpleDate() {
        return this.simpleDate;
    }

    public final void setSimpleDate(@Nullable Date var1) {
        this.simpleDate = var1;
    }

    @Nullable
    public final String getSimpleString() {
        return this.simpleString;
    }

    public final void setSimpleString(@Nullable String var1) {
        this.simpleString = var1;
    }

    @Nullable
    public final byte[] getSimpleByteArray() {
        return this.simpleByteArray;
    }

    public final void setSimpleByteArray(@Nullable byte[] var1) {
        this.simpleByteArray = var1;
    }

    @Nullable
    public final String isAnything() {
        return this.isAnything;
    }

    public final void setAnything(@Nullable String var1) {
        this.isAnything = var1;
    }

    public SimpleKotlinEntity(long arg0, @Nullable Short arg1, @Nullable Integer arg2, @Nullable Long arg3,
            @Nullable Float arg4, @Nullable Double arg5, @Nullable Boolean arg6, @Nullable Byte arg7,
            @Nullable Date arg8, @Nullable String arg9, @Nullable byte[] arg10, @Nullable String arg11) {
        this.id = arg0;
        this.simpleShort = arg1;
        this.simpleInt = arg2;
        this.simpleLong = arg3;
        this.simpleFloat = arg4;
        this.simpleDouble = arg5;
        this.simpleBoolean = arg6;
        this.simpleByte = arg7;
        this.simpleDate = arg8;
        this.simpleString = arg9;
        this.simpleByteArray = arg10;
        this.isAnything = arg11;
    }

    // methods left out as they do not affect us: copy, equals, hashCode, component1..N
}