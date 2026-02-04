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

package io.objectbox.processor;

import java.util.ServiceLoader;

import javax.annotation.processing.Processor;

/**
 * Shim to register {@link ObjectBoxProcessor} as an annotation processor.
 * <p>
 * This class must match requirements as documented in {@link Processor}.
 * <p>
 * To support "service-style" lookup by the Java compiler, it is declared in
 * {@code resources/META-INF/services/javax.annotation.processing.Processor}. See the section "Deploying service
 * providers on the class path" of {@link ServiceLoader}.
 * <p>
 * To enable <a
 * href="https://docs.gradle.org/current/userguide/java_plugin.html#sec:incremental_annotation_processing">incremental
 * annotation processing with Gradle</a>, it is declared in
 * {@code resources/META-INF/gradle/incremental.annotation.processors}.
 * <p>
 * In Gradle terms, this processor is aggregating as from each element annotated with @Entity info flows into
 * MyObjectBox file and for each element into multiple helper files (Underscore and Cursor class). Info is also
 * aggregated into the model file, but as it does not need to be compiled it doesn't matter to Gradle.
 * <p>
 * There is a flag to turn off incremental support to make indirect inheritance from entity classes work, hence the
 * processor is declared as "dynamic" and only returns the "aggregating" type in {@link #getSupportedOptions()} if
 * incremental support is enabled.
 * <p>
 * Note this class is also used in a <a
 * href="https://github.com/objectbox/objectbox-examples/blob/main/java-main-maven/README.md">Maven setup</a>, so avoid
 * renaming or moving it.
 */
public final class ObjectBoxProcessorShim extends ObjectBoxProcessor {
}
