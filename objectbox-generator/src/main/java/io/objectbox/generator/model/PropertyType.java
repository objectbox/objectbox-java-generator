/*
 * Copyright (C) 2017-2018 ObjectBox Ltd.
 *
 * This file is part of ObjectBox Build Tools.
 *
 * ObjectBox Build Tools is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * ObjectBox Build Tools is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with ObjectBox Build Tools.  If not, see <http://www.gnu.org/licenses/>.
 */

package io.objectbox.generator.model;

/**
 * Currently available types for properties.
 *
 * @author Markus
 */
public enum PropertyType {

    Boolean(true),
    Byte(true),
    Short(true),
    Char(true),
    Int(true),
    Long(true),
    Float(true),
    Double(true),
    String(false),
    ByteArray(false),
    StringArray(false),
    Date(false),
    DateNano(true),
    /** Map property containing flexible data with string keys backed by FlexBuffers, stored as byte array. */
    FlexMap(false),
    /** a long representing a ObjectBox to-one relation */
    RelationId(true);

    private final boolean scalar;

    PropertyType(boolean scalar) {
        this.scalar = scalar;
    }

    /** True if the type can be prepresented using a scalar (primitive type). */
    public boolean isScalar() {
        return scalar;
    }

}
