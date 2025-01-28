/*
 * ObjectBox Build Tools
 * Copyright (C) 2024-2025 ObjectBox Ltd.
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

package io.objectbox.generator.model

import io.objectbox.annotation.HnswFlags
import io.objectbox.annotation.HnswIndex
import io.objectbox.annotation.VectorDistanceType


/**
 * Describes HNSW index parameters for a float vector property.
 */
data class HnswParams(
    val dimensions: Long,
    val neighborsPerNode: Long?,
    val indexingSearchCount: Long?,
    val flagsExpressionSet: Set<String>,
    val distanceTypeExpression: String?,
    val reparationBacklinkProbability: Float?,
    val vectorCacheHintSizeKb: Long?
) {

    /**
     * Returns a code string for calling [io.objectbox.ModelBuilder.PropertyBuilder.hnswParams] with parameters of this.
     */
    fun getExpression(): String {
        return buildString {
            append(".hnswParams(")
            append(dimensions).append(", ")
            append(neighborsPerNode.toExpression()).append(", ")
            append(indexingSearchCount.toExpression()).append(", ")
            append(flagsExpressionSet.toExpression()).append(", ")
            append(distanceTypeExpression).append(", ")
            append(reparationBacklinkProbability.toExpression()).append(", ")
            append(vectorCacheHintSizeKb.toExpression())
            append(")")
        }
    }

    // Due to Java autoboxing need to explicitly specify number type suffix
    private fun Long?.toExpression(): String = this?.let { "${it}L" } ?: "null"
    private fun Float?.toExpression(): String = this?.let { "${it}F" } ?: "null"

    private fun Set<String>.toExpression(): String = if (isNotEmpty()) joinToString(separator = " | ") else "null"

    companion object {

        /**
         * Checks and extracts values from the [annotation] to build [HnswParams].
         *
         * @throws ModelException if invalid parameters are set.
         */
        @JvmStatic
        @Throws(ModelException::class)
        fun fromAnnotation(annotation: HnswIndex): HnswParams {
            // Reject illegal configuration values during a processor run already,
            // otherwise would error at runtime (which might not be easily attributable,
            // see ModelBuilder.java in Java library).
            // See allowed ranges in objectbox-c/src/model.cpp
            check(annotation.dimensions > 0) { "dimensions must be 1 or greater" }
            val neighborsPerNode = if (annotation.neighborsPerNode != 0L) {
                check(annotation.neighborsPerNode > 0) { "neighborsPerNode must be 1 or greater" }
                annotation.neighborsPerNode
            } else null
            val indexingSearchCount = if (annotation.indexingSearchCount != 0L) {
                check(annotation.indexingSearchCount > 0) { "indexingSearchCount must be 1 or greater" }
                annotation.indexingSearchCount
            } else null
            // 1.0 is the default, so only pass to model if different
            val reparationBacklinkProbability = if (annotation.reparationBacklinkProbability != 1.0F) {
                check(
                    annotation.reparationBacklinkProbability >= 0.0
                            && annotation.reparationBacklinkProbability < 1.0
                ) { "reparationBacklinkProbability must be between 0.0 or 1.0" }
                annotation.reparationBacklinkProbability
            } else null
            val vectorCacheHintSizeKb = if (annotation.vectorCacheHintSizeKB != 0L) {
                check(annotation.vectorCacheHintSizeKB > 0) { "vectorCacheHintSizeKB must be 1 or greater" }
                annotation.vectorCacheHintSizeKB
            } else null
            return HnswParams(
                annotation.dimensions,
                neighborsPerNode,
                indexingSearchCount,
                annotation.flags.toExpressionSet(),
                annotation.distanceType.toExpression(),
                reparationBacklinkProbability,
                vectorCacheHintSizeKb
            )
        }
    }
}

/**
 * Maps to string representation of [io.objectbox.model.HnswDistanceType] to use with code generation.
 */
fun VectorDistanceType.toExpression(): String? {
    return when (this) {
        VectorDistanceType.DEFAULT -> null
        VectorDistanceType.EUCLIDEAN -> "HnswDistanceType.Euclidean"
        VectorDistanceType.COSINE -> "HnswDistanceType.Cosine"
        VectorDistanceType.DOT_PRODUCT -> "HnswDistanceType.DotProduct"
        VectorDistanceType.GEO -> "HnswDistanceType.Geo"
        VectorDistanceType.DOT_PRODUCT_NON_NORMALIZED -> "HnswDistanceType.DotProductNonNormalized"
    }
}

/**
 * Maps to string representations of [io.objectbox.model.HnswFlags] to use with code generation.
 */
fun HnswFlags.toExpressionSet(): Set<String> {
    val set = mutableSetOf<String>()
    if (debugLogs) set.add("HnswFlags.DebugLogs")
    if (debugLogsDetailed) set.add("HnswFlags.DebugLogsDetailed")
    if (vectorCacheSimdPaddingOff) set.add("HnswFlags.VectorCacheSimdPaddingOff")
    if (reparationLimitCandidates) set.add("HnswFlags.ReparationLimitCandidates")
    return set
}

/**
 * Like Kotlin's check, but throws a [ModelException] to indicate a user-resolvable error.
 */
inline fun check(value: Boolean, lazyMessage: () -> Any) {
    if (!value) {
        val message = lazyMessage()
        throw ModelException(message.toString())
    }
}
