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

package io.objectbox.generator;

import io.objectbox.generator.model.Schema;

/** State for a job done by {@link BoxGenerator}. */
public class GeneratorJob {
    private final Schema schema;
    private final GeneratorOutput output;

    private GeneratorOutput outputFlatbuffersSchema;
    private boolean daoCompat;

    public GeneratorJob(Schema schema, GeneratorOutput output) {
        this.schema = schema;
        this.output = output;
    }

    public Schema getSchema() {
        return schema;
    }

    public GeneratorOutput getOutput() {
        return output;
    }

    public GeneratorOutput getOutputFlatbuffersSchema() {
        return outputFlatbuffersSchema;
    }

    public void setOutputFlatbuffersSchema(GeneratorOutput outputFlatbuffersSchema) {
        this.outputFlatbuffersSchema = outputFlatbuffersSchema;
    }

    public boolean isDaoCompat() {
        return daoCompat;
    }

    public void setDaoCompat(boolean daoCompat) {
        this.daoCompat = daoCompat;
    }
}
