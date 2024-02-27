/*
 * Copyright (C) 2017-2024 ObjectBox Ltd.
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

package io.objectbox.processor;

import com.google.auto.service.AutoService;

import net.ltgt.gradle.incap.IncrementalAnnotationProcessor;

import javax.annotation.processing.Processor;


import static net.ltgt.gradle.incap.IncrementalAnnotationProcessorType.DYNAMIC;

/**
 * Shim to use AutoService (it does not pick up a processor written in Kotlin).
 * <p>
 * Processor is aggregating as from each element annotated with @Entity info flows
 * into MyObjectBox file and for each element into multiple helper files (Underscore and Cursor class).
 * Info is also aggregated into the model file, but as it does not need to be compiled it doesn't matter to Gradle.
 * <p>
 * There is a flag to turn off incremental support to make indirect inheritance from entity classes work,
 * hence the processor is declared as dynamic here.
 */
@AutoService(Processor.class)
@IncrementalAnnotationProcessor(DYNAMIC)
public final class ObjectBoxProcessorShim extends ObjectBoxProcessor {
}
