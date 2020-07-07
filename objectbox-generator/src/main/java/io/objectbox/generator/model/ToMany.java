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

import java.util.List;

/** To-many relationship from a source entity to many target entities. */
public class ToMany extends ToManyBase {
    private Property[] sourceProperties;
    private final Property[] targetProperties;
    private ToOne backlinkToOne;

    public ToMany(Schema schema, Entity sourceEntity, Property[] sourceProperties, Entity targetEntity,
                  Property[] targetProperties) {
        super(schema, sourceEntity, targetEntity);
        this.sourceProperties = sourceProperties;
        this.targetProperties = targetProperties;
    }

    public Property[] getSourceProperties() {
        return sourceProperties;
    }

    public void setSourceProperties(Property[] sourceProperties) {
        this.sourceProperties = sourceProperties;
    }

    public Property[] getTargetProperties() {
        return targetProperties;
    }

    public ToOne getBacklinkToOne() {
        return backlinkToOne;
    }

    void init2ndPass() {
        super.init2ndPass();
        if (sourceProperties == null) {
            List<Property> pks = sourceEntity.getPropertiesPk();
            if (pks.isEmpty()) {
                throw new ModelRuntimeException("Source entity has no primary key, but we need it for " + this);
            }
            sourceProperties = new Property[pks.size()];
            sourceProperties = pks.toArray(sourceProperties);
        }
        int count = sourceProperties.length;
        if (count != targetProperties.length) {
            throw new ModelRuntimeException("Source properties do not match target properties: " + this);
        }

        for (int i = 0; i < count; i++) {
            Property sourceProperty = sourceProperties[i];
            Property targetProperty = targetProperties[i];

            PropertyType sourceType = sourceProperty.getPropertyType();
            PropertyType targetType = targetProperty.getPropertyType();
            if (sourceType == null || targetType == null) {
                throw new ModelRuntimeException("Property type uninitialized");
            }
            if (!sourceType.supportsRelationToTarget(targetType)) {
                throw new ModelRuntimeException("To-many property types incompatible: " + this + " (" + sourceType +
                        " vs. " + targetType + ")");
            }
        }
    }

    void init3rdPass() {
        super.init3rdPass();
        if (targetProperties.length == 1) {
            String propertyName = targetProperties[0].getPropertyName();
            for (ToOne toOne : getTargetEntity().getToOneRelations()) {
                if (toOne.getTargetEntity() == sourceEntity && (propertyName.equalsIgnoreCase(toOne.getNameToOne()) ||
                        propertyName.equalsIgnoreCase(toOne.getTargetIdProperty().getPropertyName()))) {
                    if (backlinkToOne != null) {
                        throw new ModelRuntimeException("More than one matching backlink: " + backlinkToOne + " vs. " + toOne);
                    }
                    backlinkToOne = toOne;
                }
            }
            if (backlinkToOne == null) {
                throw new ModelRuntimeException("No matching backlink found for " + this);
            }
        }
    }

}
