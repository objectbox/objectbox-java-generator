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
//        var isBoolean: Boolean? = null,
//
//        var simpleByte: Byte? = null,
//
//        var simpleDate: Date? = null,
//
//        var simpleString: String? = null,
//
//        var simpleByteArray: ByteArray? = null,
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
    private Boolean isBoolean;
    @Nullable
    private Byte simpleByte;
    @Nullable
    private Date simpleDate;
    @Nullable
    private String simpleString;
    @Nullable
    private byte[] simpleByteArray;

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
    public final Boolean isBoolean() {
        return this.isBoolean;
    }

    public final void setBoolean(@Nullable Boolean var1) {
        this.isBoolean = var1;
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

    public SimpleKotlinEntity(long arg0, @Nullable Short arg1, @Nullable Integer arg2, @Nullable Long arg3,
            @Nullable Float arg4, @Nullable Double arg5, @Nullable Boolean arg6, @Nullable Boolean arg7,
            @Nullable Byte arg8, @Nullable Date arg9, @Nullable String arg10, @Nullable byte[] arg11) {
        this.id = arg0;
        this.simpleShort = arg1;
        this.simpleInt = arg2;
        this.simpleLong = arg3;
        this.simpleFloat = arg4;
        this.simpleDouble = arg5;
        this.simpleBoolean = arg6;
        this.isBoolean = arg7;
        this.simpleByte = arg8;
        this.simpleDate = arg9;
        this.simpleString = arg10;
        this.simpleByteArray = arg11;
    }

    // methods left out as they do not affect us: copy, equals, hashCode, component1..N
}