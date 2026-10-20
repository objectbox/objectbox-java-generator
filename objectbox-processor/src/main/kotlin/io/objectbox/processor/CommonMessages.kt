/*
 * ObjectBox Build Tools
 * Copyright (C) 2026 ObjectBox Ltd.
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

package io.objectbox.processor

import io.objectbox.generator.model.HasParsedElement

/**
 * Helps raise and keep count of errors during processing, implemented for each processor API
 * (which have different logging APIs and elements to link messages to).
 */
interface CommonMessages {

    val errorRaised: Boolean

    /**
     * If debug mode is enabled, prints `message`. Otherwise does nothing.
     */
    fun debug(message: String)

    /**
     * Prints `message` as note or info.
     */
    fun info(message: String)

    /**
     * Prints `message` as error.
     */
    fun error(message: String)

    /**
     * Prints `message` as error, links to `elementHolder.parsedElement` if it is an element of
     * the processor API.
     */
    fun error(message: String, elementHolder: HasParsedElement?)

    /**
     * Prints `message` as error, links to the class.
     */
    fun error(message: String, linkToClass: ProcessingClass)

    /**
     * Prints error like `message (<qualified name>.<field name>)`, links to the field.
     */
    fun error(message: String, field: ProcessingField)

}
