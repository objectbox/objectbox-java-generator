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

/** To-one relationship from a source entity to one (or zero) target entity. */
public class ToOne implements HasParsedElement {
    private final Schema schema;
    private final Entity sourceEntity;
    private final Entity targetEntity;
    private final Property targetIdProperty;

    private String resolvedKeyJavaType;
    private boolean resolvedKeyUseEquals;
    private String name;
    private String nameToOne;
    private final boolean useFkProperty;
    private Object parsedElement;
    private boolean toOneFieldAccessible;

    public ToOne(Schema schema, Entity sourceEntity, Entity targetEntity, Property targetIdProperty, boolean useFkProperty) {
        this.schema = schema;
        this.sourceEntity = sourceEntity;
        this.targetEntity = targetEntity;
        this.targetIdProperty = targetIdProperty;
        this.useFkProperty = useFkProperty;
    }

    public Entity getSourceEntity() {
        return sourceEntity;
    }

    public Entity getTargetEntity() {
        return targetEntity;
    }

    public Property getTargetIdProperty() {
        return targetIdProperty;
    }

    public String getResolvedKeyJavaType() {
        return resolvedKeyJavaType;
    }

    public boolean getResolvedKeyUseEquals() {
        return resolvedKeyUseEquals;
    }

    public String getName() {
        return name;
    }

    /**
     * Sets the name of the relation, which is used as the property name in the entity (the source entity owning the
     * to-many relationship).
     */
    public void setName(String name) {
        this.name = name;
    }

    public boolean isUseFkProperty() {
        return useFkProperty;
    }

    public String getNameToOne() {
        return nameToOne;
    }

    public String getToOneValueExpression() {
        return toOneFieldAccessible ? nameToOne : "get" + TextUtil.capFirst(nameToOne) + "()";
    }

    public void setNameToOne(String nameToOne) {
        this.nameToOne = nameToOne;
    }

    public void setToOneFieldAccessible(boolean toOneFieldAccessible) {
        this.toOneFieldAccessible = toOneFieldAccessible;
    }

    public boolean isToOneFieldAccessible() {
        return toOneFieldAccessible;
    }

    /** Represented as just a ToOne property without a property of the target type. */
    public boolean isPlainToOne() {
        return name.equals(nameToOne);
    }

    void init2ndPass() {
        if (name == null) {
            char[] nameCharArray = targetEntity.getClassName().toCharArray();
            nameCharArray[0] = Character.toLowerCase(nameCharArray[0]);
            name = new String(nameCharArray);
        }
        if (nameToOne == null) {
            if (targetIdProperty != null) {
                nameToOne = name + "ToOne";
            } else {
                nameToOne = name;
            }
        }
    }

    /** Constructs fkColumns. Depends on 2nd pass of target key properties. */
    void init3ndPass() {
        if (targetIdProperty != null) {
            PropertyType propertyType = targetIdProperty.getPropertyType();
            if (propertyType != PropertyType.RelationId) {
                throw new ModelRuntimeException("To-one target ID property type is incompatible with a to-one relation: "
                        + propertyType);
            }
            resolvedKeyJavaType = schema.mapToJavaTypeNullable(propertyType);
            resolvedKeyUseEquals = checkUseEquals(propertyType);
        }
    }

    protected boolean checkUseEquals(PropertyType propertyType) {
        boolean useEquals;
        switch (propertyType) {
            case Byte:
            case Short:
            case Int:
            case Long:
            case Boolean:
            case Float:
                useEquals = true;
                break;
            default:
                useEquals = false;
                break;
        }
        return useEquals;
    }

    @Override
    public Object getParsedElement() {
        return parsedElement;
    }

    @Override
    public void setParsedElement(Object parsedElement) {
        this.parsedElement = parsedElement;
    }

    @Override
    public String toString() {
        String sourceName = sourceEntity != null ? sourceEntity.getClassName() : null;
        String targetName = targetEntity != null ? targetEntity.getClassName() : null;
        return "ToOne '" + name + "' from " + sourceName + " to " + targetName;
    }

}
