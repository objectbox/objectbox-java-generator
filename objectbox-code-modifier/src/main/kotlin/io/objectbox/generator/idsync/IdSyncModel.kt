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

package io.objectbox.generator.idsync

import io.objectbox.generator.IdUid

data class IdSyncModel(
        /** "Comments" in the JSON file */
        val _note1: String = "KEEP THIS FILE! Check it into a version control system (VCS) like git.",
        val _note2: String = "ObjectBox manages crucial IDs for your object model. See docs for details.",
        val _note3: String = "If you have VCS merge conflicts, you must resolve them according to ObjectBox docs.",

        val version: Long,
        var modelVersion: Long = MODEL_VERSION,
        /** Specify backward compatibility with older parsers.*/
        var modelVersionParserMinimum: Long = MODEL_VERSION,
        val lastEntityId: IdUid,
        val lastIndexId: IdUid,
        val lastRelationId: IdUid,
        // TODO use this once we support sequences
        val lastSequenceId: IdUid,

        val entities: List<Entity>,

        /**
         * Previously allocated UIDs (e.g. via "@Uid" without value) to use to provide UIDs for new entities,
         * properties, or relations.
         */
        var newUidPool: List<Long>?,

        /** Previously used UIDs, which are now deleted. Archived to ensure no collisions. */
        val retiredEntityUids: List<Long>?,

        /** Previously used UIDs, which are now deleted. Archived to ensure no collisions. */
        val retiredPropertyUids: List<Long>?,

        /** Previously used UIDs, which are now deleted. Archived to ensure no collisions. */
        val retiredIndexUids: List<Long>?,

        /** Previously used UIDs, which are now deleted. Archived to ensure no collisions. */
        val retiredRelationUids: List<Long>?
) {
    companion object {
        const val MODEL_VERSION = 5L // !! When upgrading always check MODEL_VERSION_PARSER_MINIMUM !!
        const val MODEL_VERSION_PARSER_MINIMUM = 5L
    }
}

interface HasIdUid {
    val id: IdUid

    var uid: Long
        get() = id.uid
        set(value) {
            id.uid = value
        }

    var modelId: Int
        get() = id.id
        set(value) {
            id.id = value
        }
}

data class Entity(
        val name: String,
        override val id: IdUid = IdUid(),
        val lastPropertyId: IdUid,
        val properties: List<Property>,
        val relations: List<Relation>
) : HasIdUid


data class Property(
        override val id: IdUid = IdUid(),
        val name: String,
        val indexId: IdUid?,
        val type: Int,
        val flags: Int?
) : HasIdUid

data class Relation(
        override val id: IdUid = IdUid(),
        val name: String
) : HasIdUid

