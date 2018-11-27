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

/** NOT IMPLEMENTED YET. Check back later. */
public class Query {
    @SuppressWarnings("unused")
    private String name;
    private List<QueryParam> parameters;
    @SuppressWarnings("unused")
    private boolean distinct;

    public Query(String name) {
        this.name = name;
        parameters= new ArrayList<>();
    }
    
    public QueryParam addEqualsParam(Property column) {
        return addParam(column, "=");
    }

    public QueryParam addParam(Property column, String operator) {
        QueryParam queryParam = new QueryParam(column, operator);
        parameters.add(queryParam);
        return queryParam;
    }
    
    public void distinct() {
        distinct = true;
    }


}
