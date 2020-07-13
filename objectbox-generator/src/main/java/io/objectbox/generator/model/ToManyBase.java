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

import io.objectbox.generator.TextUtil;

/** Base class for to-many relationship from source entities to target entities. */
public abstract class ToManyBase implements HasParsedElement {
    private final String name;
    protected final Entity sourceEntity;
    protected final Entity targetEntity;
    private boolean fieldAccessible;
    private Object parsedElement;

    /**
     * @param name The name of the relation, which is used as the property name in the entity
     *             (the source entity owning the to-many relationship).
     */
    ToManyBase(Entity sourceEntity, Entity targetEntity, String name) {
        if (name == null) {
            throw new IllegalArgumentException("name must not be null");
        }
        this.sourceEntity = sourceEntity;
        this.targetEntity = targetEntity;
        this.name = name;
    }

    public Entity getSourceEntity() {
        return sourceEntity;
    }

    public Entity getTargetEntity() {
        return targetEntity;
    }

    public String getName() {
        return name;
    }

    public boolean isFieldAccessible() {
        return fieldAccessible;
    }

    public void setFieldAccessible(boolean fieldAccessible) {
        this.fieldAccessible = fieldAccessible;
    }

    public String getValueExpression() {
        return fieldAccessible ? name : "get" + TextUtil.capFirst(name) + "()";
    }

    @Override
    public Object getParsedElement() {
        return parsedElement;
    }

    @Override
    public void setParsedElement(Object parsedElement) {
        this.parsedElement = parsedElement;
    }

    void init2ndPass() {
    }

    void init3rdPass() {
    }

    @Override
    public String toString() {
        String sourceName = sourceEntity != null ? sourceEntity.getClassName() : null;
        String targetName = targetEntity != null ? targetEntity.getClassName() : null;
        return "ToMany '" + name + "' from " + sourceName + " to " + targetName;
    }

}
