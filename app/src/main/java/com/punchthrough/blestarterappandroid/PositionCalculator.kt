/*
 * Copyright 2026 Punch Through Design LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.punchthrough.blestarterappandroid

import android.util.Log
import com.matt.guidebeacons.beacons.Beacon
import timber.log.Timber
import kotlin.math.abs
import kotlin.math.pow
import kotlin.math.sqrt

data class Position(
    val x: Double,
    val y: Double,
    val accuracy: Double,
    val timestamp: Long = System.currentTimeMillis()
)

interface PositionCalculator {
    fun calculatePosition(beacons: List<Beacon>): Position?
}

class IndoorPositioningCalculator : PositionCalculator {
    companion object {
        //        private const val TAG = "IndoorPositioning"
//        private const val PATH_LOSS_EXPONENT = 1.45
        private const val SMOOTHING_FACTOR = 0.9
        private const val DAMPING_FACTOR = 0.9
        private const val TOP_K_BEACONS = 10
        private const val MIN_VALID_DISTANCE = 0.001
        private const val MAX_VALID_DISTANCE = 1000.0
        //        private const val REFERENCE_RSSI = -59
//        private const val MIN_BEACONS = 3
        private const val MAX_ITERATIONS = 100
        private const val MIN_SIMILARITY = -1000.0
    }

    private val smoothedRssi = mutableMapOf<String, Double>()
    private val fingerprints = mutableListOf<Fingerprint>()
    private val weightedCentroidCalculator = WeightedCentroidCalculator()

    data class Fingerprint(
        val position: Position,
        val rssiValues: Map<String, Double>,
        val rankedBeacons: List<String>,
        val averageRssi: Double,
        val timestamp: Long = System.currentTimeMillis()
    )

    private fun exponentialSmoothing(beacons: List<Beacon>): Map<String, Double> {
        return beacons.associate { beacon ->
            // BeaconId equals string!!!!!!
            val beaconId = beacon.beaconName
            val currentRssi = beacon.getFilteredRSSI()
            val smoothedValue = SMOOTHING_FACTOR * currentRssi +
                (1 - SMOOTHING_FACTOR) * (smoothedRssi[beaconId] ?: currentRssi)
            smoothedRssi[beaconId] = smoothedValue
            beaconId to smoothedValue
        }
    }

//    private fun calculateDistanceFromRssi(rssi: Double): Double {
//        return try {
//            val ratio = (REFERENCE_RSSI - rssi) / (10 * PATH_LOSS_EXPONENT)
//            10.0.pow(ratio).coerceIn(MIN_VALID_DISTANCE, MAX_VALID_DISTANCE)
//        } catch (e: Exception) {
//            Log.e(TAG, "Error calculating distance from RSSI: ${e.message}")
//            MAX_VALID_DISTANCE
//        }
//    }

    private fun rankBeacons(smoothedData: Map<String, Double>): List<String> {
        return smoothedData.entries
            .sortedByDescending { it.value }
            .take(TOP_K_BEACONS)
            .map { it.key }
    }

    private fun createFingerprint(position: Position, beacons: List<Beacon>): Fingerprint {
        val smoothedData = exponentialSmoothing(beacons)
        val rankedBeacons = rankBeacons(smoothedData)
        val averageRssi = smoothedData.values.average()

        return Fingerprint(
            position = position,
            rssiValues = smoothedData,
            rankedBeacons = rankedBeacons,
            averageRssi = averageRssi
        )
    }

    private fun findSimilarFingerprints(
        currentFingerprint: Fingerprint,
        k: Int = 3
    ): List<Fingerprint> {
        if (fingerprints.isEmpty()) return emptyList()

        return fingerprints
            .asSequence()
            .map { fingerprint ->
                val rssiSimilarity = calculateRssiSimilarity(
                    fingerprint.rssiValues,
                    currentFingerprint.rssiValues
                )
                val beaconMatchScore = calculateBeaconMatchScore(
                    fingerprint.rankedBeacons,
                    currentFingerprint.rankedBeacons
                )
                val positionDistance = calculateDistance(
                    fingerprint.position,
                    currentFingerprint.position
                )

                Triple(fingerprint, rssiSimilarity * beaconMatchScore, positionDistance)
            }
            .filter { it.second > 0.0 && it.third < MAX_VALID_DISTANCE }
            .sortedByDescending { it.second }
            .take(k)
            .map { it.first }
            .toList()
    }

    private fun calculateRssiSimilarity(
        rssi1: Map<String, Double>,
        rssi2: Map<String, Double>
    ): Double {
        val commonBeacons = rssi1.keys.intersect(rssi2.keys)
        if (commonBeacons.isEmpty()) return 0.0

        val differences = commonBeacons.sumOf { beaconId ->
            val diff = abs((rssi1[beaconId] ?: 0.0) - (rssi2[beaconId] ?: 0.0))
            diff * diff
        }

        return 1.0 / (1.0 + sqrt(differences / commonBeacons.size))
    }

    private fun calculateBeaconMatchScore(
        beacons1: List<String>,
        beacons2: List<String>
    ): Double {
        val commonCount = beacons1.intersect(beacons2.toSet()).size
        return commonCount.toDouble() / TOP_K_BEACONS
    }

    private fun calculateDistance(pos1: Position, pos2: Position): Double {
        return sqrt(
            (pos1.x - pos2.x).pow(2) +
                (pos1.y - pos2.y).pow(2)
        )
    }

    private fun refinePosition(
        basicPosition: Position,
        beacons: List<Beacon>,
        clusterFingerprints: List<Fingerprint>? = null
    ): Position {
        // Create a new fingerprint from rudimentary basicPosition
        val currentFingerprint = createFingerprint(basicPosition, beacons)

        // Use either cluster fingerprints or find similar fingerprints
        val similarFingerprints = clusterFingerprints ?: findSimilarFingerprints(currentFingerprint)

        if (similarFingerprints.isEmpty()) {
            fingerprints.add(currentFingerprint)
            return basicPosition
        }

        // Calculate weights for each similar fingerprint
        val weights = similarFingerprints.map { fingerprint ->
            val rssiSimilarity = calculateRssiSimilarity(
                fingerprint.rssiValues,
                currentFingerprint.rssiValues
            )
            val beaconMatchScore = calculateBeaconMatchScore(
                fingerprint.rankedBeacons,
                currentFingerprint.rankedBeacons
            )
            rssiSimilarity * beaconMatchScore
        }

        // Include current measurement in the weighted average to prevent static "snapping"
        val currentWeight = 1.0 // Base weight for current measurement
        val totalWeight = weights.sum() + currentWeight
        if (totalWeight <= 0.0) return basicPosition

        // Calculate weighted average position including the current basic position
        var refinedLat = basicPosition.x * currentWeight
        var refinedLon = basicPosition.y * currentWeight

        similarFingerprints.forEachIndexed { index, fingerprint ->
            refinedLat += fingerprint.position.x * weights[index]
            refinedLon += fingerprint.position.y * weights[index]
        }

        val refinedPosition = Position(
            x = refinedLat / totalWeight,
            y = refinedLon / totalWeight,
            accuracy = calculateAccuracy(beacons, similarFingerprints)
        )

        // Update fingerprint database
        fingerprints.add(currentFingerprint)
        maintainFingerprintDatabase()

        return refinedPosition
    }

    private fun calculateAccuracy(
        beacons: List<Beacon>,
        similarFingerprints: List<Fingerprint>
    ): Double {
        val basicAccuracy = beacons.map { it.calculateDistanceGeneric(it.getFilteredRSSI().toInt(), it.getCalibrationRSSI()) }
            .filter { it > 0 }
            .average()
            .coerceIn(MIN_VALID_DISTANCE, MAX_VALID_DISTANCE)

        val fingerprintAccuracy = if (similarFingerprints.isNotEmpty()) {
            similarFingerprints.map { it.position.accuracy }.average()
        } else {
            basicAccuracy
        }

        return (basicAccuracy + fingerprintAccuracy) / 2
    }

    private fun maintainFingerprintDatabase(maxSize: Int = 1000) {
        if (fingerprints.size > maxSize) {
            // Remove oldest fingerprints
            fingerprints.sortBy { it.timestamp }
            val removeCount = fingerprints.size - maxSize
            repeat(removeCount) {
                fingerprints.removeAt(0)
            }
        }
    }

    private data class ClusterResult(
        val labels: IntArray,
        val centerIndices: List<Int>
    ) {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false

            other as ClusterResult

            if (!labels.contentEquals(other.labels)) return false
            if (centerIndices != other.centerIndices) return false

            return true
        }

        override fun hashCode(): Int {
            var result = labels.contentHashCode()
            result = 31 * result + centerIndices.hashCode()
            return result
        }
    }

    private fun performAffinityPropagation(
        fingerprints: List<Position>
    ): ClusterResult {
        val n = fingerprints.size
        if (n == 0) return ClusterResult(IntArray(0), emptyList())

        // Calculate similarity matrix
        val similarities = Array(n) { DoubleArray(n) }
        for (i in 0 until n) {
            for (j in 0 until n) {
                if (i != j) {
                    val noise = (Math.random() - 0.5) * 1e-10
                    similarities[i][j] = -calculateDistance(fingerprints[i], fingerprints[j]).pow(2) + noise
                }
            }
        }

        // Set preferences (diagonal) to median of similarities
        val allSims = similarities.flatMap { row -> row.filter { it != 0.0 } }
        val medianSim = if (allSims.isNotEmpty()) allSims.median() else MIN_SIMILARITY
        for (i in 0 until n) {
            similarities[i][i] = medianSim
        }

        // Initialize messages
        val responsibilities = Array(n) { DoubleArray(n) }
        val availabilities = Array(n) { DoubleArray(n) }

        // Perform clustering
        var iteration = 0
        var converged = false

        while (!converged && iteration < MAX_ITERATIONS) {
            // Update responsibilities
            val oldResponsibilities = responsibilities.map { it.clone() }
            updateResponsibilities(similarities, availabilities, responsibilities)

            // Update availabilities
            val oldAvailabilities = availabilities.map { it.clone() }
            updateAvailabilities(responsibilities, availabilities)

            // Apply damping
            applyDamping(responsibilities, oldResponsibilities)
            applyDamping(availabilities, oldAvailabilities)

            // Check convergence
            converged = hasConverged(oldResponsibilities, responsibilities) &&
                hasConverged(oldAvailabilities, availabilities)

            iteration++
        }

        // Identify clusters
        val labels = IntArray(n)
        val centerIndices = mutableListOf<Int>()

        for (i in 0 until n) {
            val criterion = responsibilities[i][i] + availabilities[i][i]
            if (criterion > 0) {
                centerIndices.add(i)
            }
        }

        // Assign points to clusters
        for (i in 0 until n) {
            var maxVal = Double.NEGATIVE_INFINITY
            var bestCenter = -1

            for (center in centerIndices) {
                val value = similarities[i][center]
                if (value > maxVal) {
                    maxVal = value
                    bestCenter = centerIndices.indexOf(center)
                }
            }

            labels[i] = bestCenter
        }

        return ClusterResult(labels, centerIndices)
    }

    private fun updateResponsibilities(
        similarities: Array<DoubleArray>,
        availabilities: Array<DoubleArray>,
        responsibilities: Array<DoubleArray>
    ) {
        val n = similarities.size
        for (i in 0 until n) {
            for (k in 0 until n) {
                var maxVal = Double.NEGATIVE_INFINITY
                for (kPrime in 0 until n) {
                    if (kPrime != k) {
                        maxVal = maxOf(maxVal, similarities[i][kPrime] + availabilities[i][kPrime])
                    }
                }
                responsibilities[i][k] = similarities[i][k] - maxVal
            }
        }
    }

    private fun updateAvailabilities(
        responsibilities: Array<DoubleArray>,
        availabilities: Array<DoubleArray>
    ) {
        val n = responsibilities.size
        for (i in 0 until n) {
            for (k in 0 until n) {
                if (i != k) {
                    var sum = 0.0
                    for (iPrime in 0 until n) {
                        if (iPrime != i && iPrime != k) {
                            sum += maxOf(0.0, responsibilities[iPrime][k])
                        }
                    }
                    availabilities[i][k] = minOf(0.0, responsibilities[k][k] + sum)
                } else {
                    var sum = 0.0
                    for (iPrime in 0 until n) {
                        if (iPrime != k) {
                            sum += maxOf(0.0, responsibilities[iPrime][k])
                        }
                    }
                    availabilities[i][k] = sum
                }
            }
        }
    }

    private fun applyDamping(
        current: Array<DoubleArray>,
        old: List<DoubleArray>
    ) {
        for (i in current.indices) {
            for (j in current[i].indices) {
                current[i][j] = current[i][j] * (1 - DAMPING_FACTOR) +
                    old[i][j] * DAMPING_FACTOR
            }
        }
    }

    private fun hasConverged(
        old: List<DoubleArray>,
        new: Array<DoubleArray>
    ): Boolean {
        val epsilon = 1e-6
        return old.indices.all { i ->
            old[i].indices.all { j ->
                abs(old[i][j] - new[i][j]) < epsilon
            }
        }
    }

    private fun List<Double>.median(): Double {
        if (isEmpty()) return MIN_SIMILARITY
        val sorted = sorted()
        return if (size % 2 == 0) {
            (sorted[size/2 - 1] + sorted[size/2]) / 2.0
        } else {
            sorted[size/2]
        }
    }

    // Update your existing position calculation to use clustering
    override fun calculatePosition(beacons: List<Beacon>): Position? {
        // First get basic position
        // Finds the weight based on calculated distance from beacon, weights it to the closest beacon cluster
        val basicPosition = weightedCentroidCalculator.calculatePosition(beacons) ?: return null

        // If we don't have enough fingerprints, just return basic position
        if (fingerprints.size < TOP_K_BEACONS) {
            addFingerprint(createFingerprint(basicPosition, beacons))
            return basicPosition
        }

        // Perform clustering on fingerprints
        val fingerprintPositions = fingerprints.map { it.position }
        val clusterResult = performAffinityPropagation(fingerprintPositions)

        // Find nearest cluster
        val nearestClusterIndex = clusterResult.centerIndices.minByOrNull { centerIdx ->
            calculateDistance(
                fingerprintPositions[centerIdx],
                basicPosition
            )
        } ?: return basicPosition

        // Get fingerprints from the nearest cluster
        val clusterFingerprints = fingerprints.filterIndexed { index, _ ->
            clusterResult.labels[index] == clusterResult.centerIndices.indexOf(nearestClusterIndex)
        }

        // Refine position using cluster fingerprints
        return refinePosition(basicPosition, beacons, clusterFingerprints)
    }

    //    fun clearFingerprints() {
//        fingerprints.clear()
//        smoothedRssi.clear()
//    }
    // Optional: Method to add fingerprints if needed
    private fun addFingerprint(fingerprint: Fingerprint) {
        fingerprints.add(fingerprint)
    }

}

// Keep existing WeightedCentroidCalculator implementation
class WeightedCentroidCalculator : PositionCalculator {
    companion object {
        private const val TAG = "WeightedCentroidCalc"
        private const val MIN_BEACONS = 3
        private const val MAX_VALID_DISTANCE = 1000.0 // meters
        private const val MIN_VALID_DISTANCE = 0.1 // meters
        //        private const val REFERENCE_RSSI = -59 // RSSI at 1 meter
//        private const val PATH_LOSS_EXPONENT = 2.0
        private const val DEFAULT_ACCURACY = 10.0
    }

    override fun calculatePosition(beacons: List<Beacon>): Position? {
        if (beacons.isEmpty()) {
            Timber.tag(TAG).d("No beacons provided")
            return null
        }

        // Fix and validate distances
        val validBeacons = beacons.mapNotNull { beacon ->
            val distance = getValidDistance(beacon)
            if (distance != null) {
                Timber.tag(TAG).d(
                    """
                    Weight Calculation:
                    Beacon: ${beacon.beaconName}
                    RSSI: ${beacon.getFilteredRSSI()}
                    Original Distance: ${beacon.calculateDistanceGeneric(beacon.getFilteredRSSI().toInt(), beacon.getCalibrationRSSI())}
                    Calculated Distance: $distance
                    Lat: ${beacon.getCoordinates()[0]}
                    Lon: ${beacon.getCoordinates()[1]}
                """.trimIndent()
//                    """
//                    Valid Beacon:
//                    UUID: ${beacon.uuid}
//                    RSSI: ${beacon.rssi}
//                    Original Distance: ${beacon.distance}
//                    Calculated Distance: $distance
//                    Lat: ${beacon.x}
//                    Lon: ${beacon.y}
//                """.trimIndent()
                )
                beacon to distance
            } else {
                Timber.tag(TAG).d("Invalid beacon: ${beacon.beaconName}")
                null
            }
        }

        if (validBeacons.size < MIN_BEACONS) {
            Timber.tag(TAG).d("Not enough valid beacons: ${validBeacons.size}")
            return null
        }

        try {
            var totalWeight = 0.0
            var weightedLat = 0.0
            var weightedLon = 0.0

            validBeacons.forEach { (beacon, distance) ->
                // Use inverse square of distance as weight
                val weight = 1.0 / (distance.pow(2) + 0.1) // Add small constant to prevent division by zero
                weightedLat += beacon.getCoordinates()[0] * weight
                weightedLon += beacon.getCoordinates()[1] * weight
                totalWeight += weight

//                Log.d(TAG, """
//                    Weight Calculation:
//                    Beacon: ${beacon.uuid}
//                    Distance: $distance
//                    Weight: $weight
//                    Weighted Lat: ${beacon.x * weight}
//                    Weighted Lon: ${beacon.y * weight}
//                """.trimIndent())
                Timber.tag(TAG).d(
                    """
                    Weight Calculation:
                    Beacon: ${beacon.beaconName}
                    Distance: $distance
                    Weight: $weight
                    Weighted Lat: ${beacon.getCoordinates()[0] * weight}
                    Weighted Lon: ${beacon.getCoordinates()[1] * weight}
                """.trimIndent()
                )
            }

            if (totalWeight <= 0.0) {
                Log.e(TAG, "Total weight is zero or negative")
                return null
            }

            val finalLat = weightedLat / totalWeight
            val finalLon = weightedLon / totalWeight

            if (!isValidCoordinate(finalLat, finalLon)) {
                Log.e(TAG, "Invalid coordinates calculated: $finalLat, $finalLon")
                return null
            }

            val accuracy = calculateAccuracy(validBeacons)

            return Position(
                x = finalLat,
                y = finalLon,
                accuracy = accuracy
            ).also {
                Log.d(TAG, "Final Position: lat=${it.x}, lon=${it.y}, accuracy=${it.accuracy}")
            }

        } catch (e: Exception) {
            Log.e(TAG, "Error calculating position: ${e.message}")
            return null
        }
    }

    private fun getValidDistance(beacon: Beacon): Double? {
        // First check if the beacon has valid coordinates
        if (!isValidCoordinate(beacon.getCoordinates()[0], beacon.getCoordinates()[1])) {
            return null
        }

        // Calculate distance from RSSI preserving Double precision to prevent static jumps
        val rssi = beacon.getFilteredRSSI()
        val calRssi = beacon.getCalibrationRSSI().toDouble()
        // Using common log-distance path loss model: RSSI = TxPower - 10 * n * log10(d)
        // d = 10 ^ ((TxPower - RSSI) / (10 * n))
        // Here we use calibrationRSSI as TxPower at 1m and assume path loss exponent n = 2.0 (standard indoor)
        val distance = 10.0.pow((calRssi - rssi) / 20.0)

        return distance
    }

    private fun calculateAccuracy(validBeacons: List<Pair<Beacon, Double>>): Double {
        if (validBeacons.isEmpty()) return DEFAULT_ACCURACY

        try {
            // Use the known distances for accuracy calculation
            val distances = validBeacons.map { it.second }
            val meanDistance = distances.average()

            // Calculate standard deviation of distances
            val variance = distances.map { (it - meanDistance).pow(2) }.average()
            val stdDev = sqrt(variance)

            return stdDev.coerceIn(MIN_VALID_DISTANCE, MAX_VALID_DISTANCE)
        } catch (e: Exception) {
            Log.e(TAG, "Error calculating accuracy: ${e.message}")
            return DEFAULT_ACCURACY
        }
    }

    private fun isValidCoordinate(lat: Double, lon: Double): Boolean {
        return lat in -90.0..90.0 &&
            lon in -180.0..180.0 &&
            lat.isFinite() &&
            lon.isFinite() &&
            (lat != 0.0 || lon != 0.0)
    }
}

object PositionCalculatorFactory {
    fun getCalculator(type: CalculatorType = CalculatorType.INDOOR_POSITIONING): PositionCalculator {
        return when (type) {
            CalculatorType.WEIGHTED_CENTROID -> WeightedCentroidCalculator()
            CalculatorType.INDOOR_POSITIONING -> IndoorPositioningCalculator()
        }
    }

    enum class CalculatorType {
        WEIGHTED_CENTROID,
        INDOOR_POSITIONING
    }
}