/*
 * ObjectBox Build Tools
 * Copyright (C) 2017-2025 ObjectBox Ltd.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published
 * by the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package io.objectbox.generator.model;

/**
 * Currently available types for properties in the Java generator.
 * <p>
 * Based on {@link io.objectbox.model.PropertyType}.
 */
public enum PropertyType {

    // Ordered like in io.objectbox.model.PropertyType
    Boolean(true),
    Byte(true),
    Short(true),
    Char(true),
    Int(true),
    Long(true),
    Float(true),
    Double(true),
    String(false),
    Date(false),
    /** A long representing a ObjectBox to-one relation. */
    RelationId(true),
    DateNano(true),
    /** Property containing flexible data, e.g. a map with string keys backed by FlexBuffers, stored as byte array. */
    Flex(false),
    BooleanArray(false),
    ByteArray(false),
    ShortArray(false),
    CharArray(false),
    IntArray(false),
    LongArray(false),
    FloatArray(false),
    DoubleArray(false),
    StringArray(false);

    private final boolean scalar;

    PropertyType(boolean scalar) {
        this.scalar = scalar;
    }

    /** True if the type can be represented using a scalar (primitive type). */
    public boolean isScalar() {
        return scalar;
    }

}
