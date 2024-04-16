/*
 * ObjectBox Build Tools
 * Copyright (C) 2017-2024 ObjectBox Ltd.
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

import io.objectbox.generator.TextUtil;

/** Base class for to-many relationship from source entities to target entities. */
public abstract class ToManyBase implements HasParsedElement {
    private final String name;
    private final String targetEntityName;
    private final boolean fieldAccessible;

    private Entity sourceEntity;
    private Entity targetEntity;
    private Object parsedElement;

    /**
     * @param name The name of the relation, which is used as the property name in the entity
     *             (the source entity owning the to-many relationship).
     */
    ToManyBase(String name, String targetEntityName, boolean isFieldAccessible) {
        if (name == null) {
            throw new IllegalArgumentException("name must not be null");
        }
        if (targetEntityName == null) {
            throw new IllegalArgumentException("targetEntityName must not be null");
        }
        this.name = name;
        this.targetEntityName = targetEntityName;
        this.fieldAccessible = isFieldAccessible;
    }

    public String getTargetEntityName() {
        return targetEntityName;
    }

    public Entity getSourceEntity() {
        return sourceEntity;
    }

    public Entity getTargetEntity() {
        return targetEntity;
    }

    void setSourceAndTargetEntity(Entity sourceEntity, Entity targetEntity) {
        this.sourceEntity = sourceEntity;
        this.targetEntity = targetEntity;
    }

    public String getName() {
        return name;
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

    void init3rdPass() {
        if (sourceEntity == null || targetEntity == null) {
            throw new IllegalStateException("Source and target entity are not set for " + this + ".");
        }
    }

    @Override
    public String toString() {
        String sourceName = sourceEntity != null ? sourceEntity.getClassName() : null;
        String targetName = targetEntity != null ? targetEntity.getClassName() : null;
        return "ToMany '" + name + "' from " + sourceName + " to " + targetName;
    }

}
