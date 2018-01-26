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

package io.objectbox.generator;

import io.objectbox.generator.model.Schema;

/** State for a job done by {@link BoxGenerator}. */
public class GeneratorJob {
    private final Schema schema;
    private final GeneratorOutput output;

    private GeneratorOutput outputTest;
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

    public GeneratorOutput getOutputTest() {
        return outputTest;
    }

    public void setOutputTest(GeneratorOutput outputTest) {
        this.outputTest = outputTest;
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
