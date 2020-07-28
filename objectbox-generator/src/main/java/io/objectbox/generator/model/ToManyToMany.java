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

/** To-many relationship from many source entities to many target entities. */
@SuppressWarnings("unused")
public class ToManyToMany extends ToManyBase {
    private Property[] sourceProperties; // together uniquely identify source entity
    private String linkedToManyName; // the name of the ToMany this ToMany is supposed to link to
    private ToManyStandalone backlinkToMany; // the ToMany this ToMany is linked to

    public ToManyToMany(Schema schema, Entity sourceEntity, Entity targetEntity,
            String linkedToManyName) {
        super(schema, sourceEntity, targetEntity);
        this.linkedToManyName = linkedToManyName;
    }

    public Property[] getSourceProperties() {
        return sourceProperties;
    }

    public void setSourceProperties(Property[] sourceProperties) {
        this.sourceProperties = sourceProperties;
    }

    public String getLinkedToManyName() {
        return linkedToManyName;
    }

    public ToManyStandalone getBacklinkToMany() {
        return backlinkToMany;
    }

    void init2ndPass() {
        super.init2ndPass();

        List<Property> pks = sourceEntity.getPropertiesPk();
        if (pks.isEmpty()) {
            throw new ModelRuntimeException("Source entity has no primary key, but we need it for " + this);
        }
        sourceProperties = new Property[pks.size()];
        sourceProperties = pks.toArray(sourceProperties);

        if (linkedToManyName == null || linkedToManyName.length() == 0) {
            throw new ModelRuntimeException("Linked ToMany name not specified");
        }
    }

    void init3rdPass() {
        super.init3rdPass();
        // check if there actually is a ToMany in source entity that can be linked to
        for (ToManyBase toMany : getTargetEntity().getToManyRelations()) {
            if (toMany.getTargetEntity() == sourceEntity
                    && linkedToManyName.equalsIgnoreCase(toMany.getName())
                    && toMany instanceof ToManyStandalone) {
                if (backlinkToMany != null) {
                    throw new ModelRuntimeException("More than one matching backlink: " + backlinkToMany + " vs. " + toMany);
                }
                backlinkToMany = (ToManyStandalone) toMany;
            }
        }
        if (backlinkToMany == null) {
            throw new ModelRuntimeException("No matching backlink found for " + this);
        }
    }

}
