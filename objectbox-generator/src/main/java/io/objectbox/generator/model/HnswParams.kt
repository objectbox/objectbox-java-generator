/*
 * ObjectBox Build Tools
 * Copyright (C) 2024-2026 ObjectBox Ltd.
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
import io.objectbox.annotation.VectorBuildLevel
import io.objectbox.annotation.VectorDistanceType
import io.objectbox.annotation.VectorQuantization
import io.objectbox.annotation.VectorSearchLevel


/**
 * Describes HNSW index parameters for a float vector property.
 *
 * The parameters added with ObjectBox 6.0 (quantization and the ones following it) are null or DEFAULT if not set;
 * setting any of them generates code using `io.objectbox.HnswIndexParams` (see [usesIndexParams]).
 */
data class HnswParams(
    val dimensions: Long,
    val neighborsPerNode: Long?,
    val indexingSearchCount: Long?,
    val flagsExpressionSet: Set<String>,
    val distanceType: VectorDistanceType,
    val reparationBacklinkProbability: Float?,
    val vectorCacheHintSizeKb: Long?,
    val quantization: VectorQuantization = VectorQuantization.DEFAULT,
    val quantizationOversample: Float? = null,
    val quantizationRescoreFactor: Float? = null,
    val searchLevel: VectorSearchLevel = VectorSearchLevel.DEFAULT,
    val buildLevel: VectorBuildLevel = VectorBuildLevel.DEFAULT,
    val bruteForceMaxCandidates: Long? = null,
    val filteredSearchMaxNodes: Long? = null
) {

    /**
     * The distance type as [io.objectbox.model.HnswDistanceType] constant, or null for the default.
     */
    val distanceTypeExpression: String?
        get() = distanceType.toExpression()

    /**
     * If any parameter added with ObjectBox 6.0 is set, which the `hnswParams()` variant with single parameters does
     * not support.
     */
    val usesIndexParams: Boolean
        get() = quantization != VectorQuantization.DEFAULT
                || quantizationOversample != null
                || quantizationRescoreFactor != null
                || searchLevel != VectorSearchLevel.DEFAULT
                || buildLevel != VectorBuildLevel.DEFAULT
                || bruteForceMaxCandidates != null
                || filteredSearchMaxNodes != null

    /**
     * The classes that [getExpression] uses in addition to [io.objectbox.model.HnswFlags] and
     * [io.objectbox.model.HnswDistanceType], which are always imported.
     */
    fun getImports(): Set<String> {
        if (!usesIndexParams) return emptySet()
        val imports = mutableSetOf("io.objectbox.HnswIndexParams")
        if (distanceType != VectorDistanceType.DEFAULT) imports.add(VectorDistanceType::class.java.name)
        if (quantization != VectorQuantization.DEFAULT) imports.add(VectorQuantization::class.java.name)
        if (searchLevel != VectorSearchLevel.DEFAULT) imports.add(VectorSearchLevel::class.java.name)
        if (buildLevel != VectorBuildLevel.DEFAULT) imports.add(VectorBuildLevel::class.java.name)
        return imports
    }

    /**
     * Returns a code string for calling [io.objectbox.ModelBuilder.PropertyBuilder.hnswParams] with parameters of this.
     *
     * To keep generated code unchanged for existing annotations, this uses the variant with single parameters unless
     * [usesIndexParams].
     */
    fun getExpression(): String {
        if (usesIndexParams) return getIndexParamsExpression()
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

    /**
     * Returns a code string for calling [io.objectbox.ModelBuilder.PropertyBuilder.hnswParams] with a
     * `HnswIndexParams` that sets only the given parameters.
     */
    private fun getIndexParamsExpression(): String {
        return buildString {
            append(".hnswParams(new HnswIndexParams(").append(dimensions).append(")")
            neighborsPerNode?.let { append(".neighborsPerNode(").append(it.toExpression()).append(")") }
            indexingSearchCount?.let { append(".indexingSearchCount(").append(it.toExpression()).append(")") }
            if (flagsExpressionSet.isNotEmpty()) append(".flags(").append(flagsExpressionSet.toExpression()).append(")")
            if (distanceType != VectorDistanceType.DEFAULT) {
                append(".distanceType(VectorDistanceType.").append(distanceType.name).append(")")
            }
            reparationBacklinkProbability?.let {
                append(".reparationBacklinkProbability(").append(it.toExpression()).append(")")
            }
            vectorCacheHintSizeKb?.let { append(".vectorCacheHintSizeKb(").append(it.toExpression()).append(")") }
            if (quantization != VectorQuantization.DEFAULT) {
                append(".quantization(VectorQuantization.").append(quantization.name).append(")")
            }
            quantizationOversample?.let {
                append(".quantizationOversample(").append(it.toExpression()).append(")")
            }
            quantizationRescoreFactor?.let {
                append(".quantizationRescoreFactor(").append(it.toExpression()).append(")")
            }
            if (searchLevel != VectorSearchLevel.DEFAULT) {
                append(".searchLevel(VectorSearchLevel.").append(searchLevel.name).append(")")
            }
            if (buildLevel != VectorBuildLevel.DEFAULT) {
                append(".buildLevel(VectorBuildLevel.").append(buildLevel.name).append(")")
            }
            bruteForceMaxCandidates?.let {
                append(".bruteForceMaxCandidates(").append(it.toExpression()).append(")")
            }
            filteredSearchMaxNodes?.let { append(".filteredSearchMaxNodes(").append(it.toExpression()).append(")") }
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
            // See allowed ranges in objectbox-c/src/model.cpp (the C model API) and, for the factors,
            // HnswSearchParams::verifyFactor() of the core.
            if (annotation.flags.lateInit) {
                // Late init: the dimensions may be given at runtime
                check(annotation.dimensions >= 0) { "dimensions must not be negative" }
            } else {
                check(annotation.dimensions > 0) {
                    "dimensions must be 1 or greater (or use late init: @HnswFlags(lateInit = true))"
                }
            }
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
            val quantizationOversample = factorOrNull(annotation.quantizationOversample, "quantizationOversample")
            val quantizationRescoreFactor =
                factorOrNull(annotation.quantizationRescoreFactor, "quantizationRescoreFactor")
            val bruteForceMaxCandidates = if (annotation.bruteForceMaxCandidates != 0L) {
                check(annotation.bruteForceMaxCandidates > 0) { "bruteForceMaxCandidates must not be negative" }
                checkFitsUint32(annotation.bruteForceMaxCandidates, "bruteForceMaxCandidates")
                annotation.bruteForceMaxCandidates
            } else null
            val filteredSearchMaxNodes = if (annotation.filteredSearchMaxNodes != 0L) {
                check(annotation.filteredSearchMaxNodes > 0) { "filteredSearchMaxNodes must not be negative" }
                checkFitsUint32(annotation.filteredSearchMaxNodes, "filteredSearchMaxNodes")
                annotation.filteredSearchMaxNodes
            } else null
            // The database supports quantization for all distance types except GEO (see
            // HnswIndex::createQuantizer() of the core); by default, it does not quantize GEO vectors.
            check(
                annotation.distanceType != VectorDistanceType.GEO
                        || annotation.quantization == VectorQuantization.DEFAULT
                        || annotation.quantization == VectorQuantization.NONE
            ) { "quantization ${annotation.quantization} is not supported with distanceType GEO, use NONE or DEFAULT" }
            if (annotation.flags.lateInit && annotation.dimensions == 0L) {
                // Without dimensions, the database ignores the other parameters of the model (it persists only the
                // flags): the late init (BoxStore.initHnswIndex()) sets all of them.
                val ignoredParams = mapOf(
                    "neighborsPerNode" to (neighborsPerNode != null),
                    "indexingSearchCount" to (indexingSearchCount != null),
                    "distanceType" to (annotation.distanceType != VectorDistanceType.DEFAULT),
                    "reparationBacklinkProbability" to (reparationBacklinkProbability != null),
                    "vectorCacheHintSizeKB" to (vectorCacheHintSizeKb != null),
                    "quantization" to (annotation.quantization != VectorQuantization.DEFAULT),
                    "quantizationOversample" to (quantizationOversample != null),
                    "quantizationRescoreFactor" to (quantizationRescoreFactor != null),
                    "searchLevel" to (annotation.searchLevel != VectorSearchLevel.DEFAULT),
                    "buildLevel" to (annotation.buildLevel != VectorBuildLevel.DEFAULT),
                    "bruteForceMaxCandidates" to (bruteForceMaxCandidates != null),
                    "filteredSearchMaxNodes" to (filteredSearchMaxNodes != null),
                    "debugLogs" to annotation.flags.debugLogs,
                    "debugLogsDetailed" to annotation.flags.debugLogsDetailed,
                    "vectorCacheSimdPaddingOff" to annotation.flags.vectorCacheSimdPaddingOff
                ).filterValues { it }.keys
                check(ignoredParams.isEmpty()) {
                    "late init without dimensions takes the parameters from BoxStore.initHnswIndex() only," +
                            " remove these or set the dimensions: ${ignoredParams.joinToString()}"
                }
            }
            return HnswParams(
                annotation.dimensions,
                neighborsPerNode,
                indexingSearchCount,
                annotation.flags.toExpressionSet(),
                annotation.distanceType,
                reparationBacklinkProbability,
                vectorCacheHintSizeKb,
                annotation.quantization,
                quantizationOversample,
                quantizationRescoreFactor,
                annotation.searchLevel,
                annotation.buildLevel,
                bruteForceMaxCandidates,
                filteredSearchMaxNodes
            )
        }

        /**
         * The maximum of an oversample or rescore factor, see HnswSearchParams::verifyFactor() of the core.
         */
        private const val MAX_FACTOR = 1_000_000F

        /**
         * The maximum value of an unsigned 32-bit field of the model (FlatBuffers `uint`); the model builder casts
         * larger values to int, which would silently change them.
         */
        private const val MAX_UINT32 = 0xFFFF_FFFFL

        /**
         * Returns null for 0 (the default), otherwise checks that the factor is from 1 to [MAX_FACTOR], which also
         * rejects NaN and infinity.
         */
        private fun factorOrNull(value: Float, name: String): Float? {
            if (value == 0.0F) return null
            check(value >= 1.0F && value <= MAX_FACTOR) { "$name must be 0 or from 1 to ${MAX_FACTOR.toLong()}" }
            return value
        }

        private fun checkFitsUint32(value: Long, name: String) {
            check(value <= MAX_UINT32) { "$name must be at most $MAX_UINT32" }
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
@Suppress("DEPRECATION") // reparationLimitCandidates: ignored by the database since 6.0, but passing it is harmless
fun HnswFlags.toExpressionSet(): Set<String> {
    val set = mutableSetOf<String>()
    if (debugLogs) set.add("HnswFlags.DebugLogs")
    if (debugLogsDetailed) set.add("HnswFlags.DebugLogsDetailed")
    if (vectorCacheSimdPaddingOff) set.add("HnswFlags.VectorCacheSimdPaddingOff")
    if (reparationLimitCandidates) set.add("HnswFlags.ReparationLimitCandidates")
    if (lateInit) set.add("HnswFlags.LateInit")
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
