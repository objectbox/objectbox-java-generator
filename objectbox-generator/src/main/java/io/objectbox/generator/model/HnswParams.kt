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

import io.objectbox.HnswIndexParams
import io.objectbox.annotation.HnswFlags
import io.objectbox.annotation.HnswIndex
import io.objectbox.annotation.VectorBuildLevel
import io.objectbox.annotation.VectorDistanceType
import io.objectbox.annotation.VectorQuantization
import io.objectbox.annotation.VectorSearchLevel
import io.objectbox.model.HnswFlags as ModelHnswFlags


/**
 * Describes HNSW index parameters for a float vector property.
 *
 * Optional parameters are null or DEFAULT if not set; the generated code sets only the other ones (see
 * [getExpression]).
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
     * The classes to import for [getExpression]: [io.objectbox.HnswIndexParams] and only the classes of the set
     * parameters.
     */
    fun getImports(): Set<String> {
        val imports = mutableSetOf(HnswIndexParams::class.java.name)
        if (flagsExpressionSet.isNotEmpty()) imports.add(ModelHnswFlags::class.java.name)
        if (distanceType != VectorDistanceType.DEFAULT) imports.add(VectorDistanceType::class.java.name)
        if (quantization != VectorQuantization.DEFAULT) imports.add(VectorQuantization::class.java.name)
        if (searchLevel != VectorSearchLevel.DEFAULT) imports.add(VectorSearchLevel::class.java.name)
        if (buildLevel != VectorBuildLevel.DEFAULT) imports.add(VectorBuildLevel::class.java.name)
        return imports
    }

    /**
     * Returns a code string for calling [io.objectbox.ModelBuilder.PropertyBuilder.hnswParams] with a
     * `HnswIndexParams` that sets only the given parameters.
     *
     * Each parameter call is on its own line, indented relative to the `.hnswParams(` call that the
     * MyObjectBox template indents by [CALL_INDENT].
     */
    fun getExpression(): String {
        val calls = mutableListOf<String>()
        neighborsPerNode?.let { calls.add("neighborsPerNode(${it.toExpression()})") }
        indexingSearchCount?.let { calls.add("indexingSearchCount(${it.toExpression()})") }
        if (flagsExpressionSet.isNotEmpty()) calls.add("flags(${flagsExpressionSet.toExpression()})")
        if (distanceType != VectorDistanceType.DEFAULT) {
            calls.add("distanceType(VectorDistanceType.${distanceType.name})")
        }
        reparationBacklinkProbability?.let { calls.add("reparationBacklinkProbability(${it.toExpression()})") }
        vectorCacheHintSizeKb?.let { calls.add("vectorCacheHintSizeKb(${it.toExpression()})") }
        if (quantization != VectorQuantization.DEFAULT) {
            calls.add("quantization(VectorQuantization.${quantization.name})")
        }
        quantizationOversample?.let { calls.add("quantizationOversample(${it.toExpression()})") }
        quantizationRescoreFactor?.let { calls.add("quantizationRescoreFactor(${it.toExpression()})") }
        if (searchLevel != VectorSearchLevel.DEFAULT) calls.add("searchLevel(VectorSearchLevel.${searchLevel.name})")
        if (buildLevel != VectorBuildLevel.DEFAULT) calls.add("buildLevel(VectorBuildLevel.${buildLevel.name})")
        bruteForceMaxCandidates?.let { calls.add("bruteForceMaxCandidates(${it.toExpression()})") }
        filteredSearchMaxNodes?.let { calls.add("filteredSearchMaxNodes(${it.toExpression()})") }
        return buildString {
            append(".hnswParams(new HnswIndexParams(").append(dimensions).append(")")
            calls.forEach { append("\n").append(CALL_INDENT).append(CHAINED_CALL_INDENT).append(".").append(it) }
            append(")")
        }
    }

    // Due to Java autoboxing need to explicitly specify number type suffix
    private fun Long.toExpression(): String = "${this}L"
    private fun Float.toExpression(): String = "${this}F"

    private fun Set<String>.toExpression(): String = joinToString(separator = " | ")

    companion object {

        /**
         * The indent of the property builder calls in the MyObjectBox template, like `.hnswParams(`.
         */
        private const val CALL_INDENT = "                "

        /**
         * The additional indent of the calls chained to `new HnswIndexParams()`.
         */
        private const val CHAINED_CALL_INDENT = "        "

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
            checkFitsUint32(annotation.dimensions, "dimensions")
            val neighborsPerNode = if (annotation.neighborsPerNode != 0L) {
                check(annotation.neighborsPerNode > 0) { "neighborsPerNode must be 1 or greater" }
                checkFitsUint32(annotation.neighborsPerNode, "neighborsPerNode")
                annotation.neighborsPerNode
            } else null
            val indexingSearchCount = if (annotation.indexingSearchCount != 0L) {
                check(annotation.indexingSearchCount > 0) { "indexingSearchCount must be 1 or greater" }
                checkFitsUint32(annotation.indexingSearchCount, "indexingSearchCount")
                annotation.indexingSearchCount
            } else null
            // 1.0 is the default, so only pass to model if different
            val reparationBacklinkProbability = if (annotation.reparationBacklinkProbability != 1.0F) {
                check(
                    annotation.reparationBacklinkProbability in 0.0..<1.0
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
 * Maps to string representations of [io.objectbox.model.HnswFlags] to use with code generation.
 */
fun HnswFlags.toExpressionSet(): Set<String> {
    val set = mutableSetOf<String>()
    if (debugLogs) set.add("HnswFlags.DebugLogs")
    if (debugLogsDetailed) set.add("HnswFlags.DebugLogsDetailed")
    if (vectorCacheSimdPaddingOff) set.add("HnswFlags.VectorCacheSimdPaddingOff")
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
