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

import java.util.ArrayList;
import java.util.List;

import io.objectbox.model.PropertyFlags;

public class Index {
    private final List<Property> properties;
    /** Value mapped to a {@link PropertyFlags} constant in Java API. */
    private int indexFlag;
    private boolean unique;
    private int uniqueOnConflictFlag;
    /** Used to restrict index value length for String and byte[] if using value based index. */
    private int maxValueLength;

    public Index(Property property) {
        properties = new ArrayList<>();
        addProperty(property);
    }

    public void addProperty(Property property) {
        properties.add(property);
    }

    public List<Property> getProperties() {
        return properties;
    }

    public int getIndexFlag() {
        return indexFlag == 0 ? PropertyFlags.INDEXED : indexFlag;
    }

    public void setIndexFlag(int flag) {
        this.indexFlag = flag;
    }

    public void makeUnique(int uniqueOnConflictFlag) {
        unique = true;
        this.uniqueOnConflictFlag = uniqueOnConflictFlag;
    }

    public boolean isUnique() {
        return unique;
    }

    public int getUniqueOnConflictFlag() {
        return uniqueOnConflictFlag;
    }

    public int getMaxValueLength() {
        return maxValueLength;
    }

    public void setMaxValueLength(int maxValueLength) {
        this.maxValueLength = maxValueLength;
    }
}
