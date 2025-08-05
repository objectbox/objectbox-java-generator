/*
 * ObjectBox Build Tools
 * Copyright (C) 2017-2025 ObjectBox Ltd.
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

package io.objectbox.generator.idsync

import com.squareup.moshi.JsonClass
import io.objectbox.generator.IdUid

@JsonClass(generateAdapter = true)
data class IdSyncModel(
    /** "Comments" in the JSON file */
    val _note1: String = "KEEP THIS FILE! Check it into a version control system (VCS) like git.",
    val _note2: String = "ObjectBox manages crucial IDs for your object model. See docs for details.",
    val _note3: String = "If you have VCS merge conflicts, you must resolve them according to ObjectBox docs.",

    val entities: List<Entity>,

    val lastEntityId: IdUid,
    val lastIndexId: IdUid,
    val lastRelationId: IdUid?,
    // Not in use, sequences are not supported, yet
    val lastSequenceId: IdUid,

    var modelVersion: Long = MODEL_VERSION,
    /** Specify backward compatibility with older parsers.*/
    var modelVersionParserMinimum: Long?,

    /**
     * Previously allocated UIDs (e.g. via "@Uid" without value) to use to provide UIDs for new entities,
     * properties, or relations.
     */
    var newUidPool: List<Long>?,

    /** Previously used UIDs, which are now deleted. Archived to ensure no collisions. */
    val retiredEntityUids: List<Long>?,

    /** Previously used UIDs, which are now deleted. Archived to ensure no collisions. */
    val retiredIndexUids: List<Long>?,

    /** Previously used UIDs, which are now deleted. Archived to ensure no collisions. */
    val retiredPropertyUids: List<Long>?,

    /** Previously used UIDs, which are now deleted. Archived to ensure no collisions. */
    val retiredRelationUids: List<Long>?,

    /** User specified version. */
    val version: Long
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

@JsonClass(generateAdapter = true)
data class Entity(
    override val id: IdUid = IdUid(),
    val lastPropertyId: IdUid,
    val name: String,
    val externalName: String?,
    val flags: Int?,
    val properties: List<Property>,
    val relations: List<Relation>?
) : HasIdUid

@JsonClass(generateAdapter = true)
data class Property(
    override val id: IdUid = IdUid(),
    val name: String,
    val indexId: IdUid?,
    val type: Int?,
    val externalName: String?,
    val externalType: Int?,
    val flags: Int?,
    val relationTarget: String?
) : HasIdUid

@JsonClass(generateAdapter = true)
data class Relation(
    override val id: IdUid = IdUid(),
    val name: String,
    val externalName: String?,
    val externalType: Int?,
    val targetId: IdUid?
) : HasIdUid
