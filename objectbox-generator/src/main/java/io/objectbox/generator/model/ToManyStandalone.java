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

import io.objectbox.generator.IdUid;
import io.objectbox.generator.TextUtil;

/** To-many relationship from a source entity to many target entities. */
public class ToManyStandalone extends ToManyBase {

    private IdUid modelId;
    private String dbName;

    public ToManyStandalone(Entity sourceEntity, Entity targetEntity, String name) {
        super(sourceEntity, targetEntity, name);
    }

    public IdUid getModelId() {
        return modelId;
    }

    public void setModelId(IdUid modelId) {
        this.modelId = modelId;
    }

    public String getDbName() {
        return dbName;
    }

    public void setDbName(String dbName) {
        this.dbName = dbName;
    }

    void init2ndPass() {
        super.init2ndPass();
        if (getDbName() == null) {
            setDbName(getName());
        }
    }

    void init3rdPass() {
        super.init3rdPass();
    }

}
