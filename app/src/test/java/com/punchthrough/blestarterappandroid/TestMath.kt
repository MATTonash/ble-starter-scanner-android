/*
 * Copyright 2024 Punch Through Design LLC
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

import org.junit.Assert.assertTrue
import org.junit.Test
import org.apache.commons.math3.distribution.TDistribution

import kotlin.random.Random

import kotlin.math.sqrt
import kotlin.math.pow
import kotlin.math.log10


/**
 * Local unit tests, which will execute on the development machine (host).
 */

private const val ACCEPTABLE_ERROR_RATE = 0.1 // e.g. 0.1 means up to 10% of all individual scenarios can fail
private const val ACCEPTABLE_TOLERANCE = 0.5 // in m
private const val CHECKS_PER_TEST = 10000
private const val DISTANCE_FROM_PREV = 0.5 // in m
private const val MAX_NUM_BEACONS = 4 // currently the 4 beacon locations are hardcoded so changing just this constant will cause errors
private const val USER_LOCATION_SQUARE = 5.0 // in m
// Above constant means the user's coordinates can be (x,y) where 0 <= x,y <= USER_LOCATION_SQUARE




class TestMath {
    val coordinates = Array(MAX_NUM_BEACONS) { DoubleArray(3) }

    // To get sample RSSI values
    var RSSISampleSize = 100.0
    var RSSIVariance = 25.0
    var tdist = TDistribution(RSSISampleSize)

    init {
        coordinates[0] = doubleArrayOf(USER_LOCATION_SQUARE, USER_LOCATION_SQUARE, 0.0)
        coordinates[1] = doubleArrayOf(USER_LOCATION_SQUARE, 0.0, 0.0)
        coordinates[2] = doubleArrayOf(0.0, USER_LOCATION_SQUARE, 0.0)
        coordinates[3] = doubleArrayOf(0.0, 0.0, 0.0)

    }

    @Test
    fun test3DMultilaterationWithNoErrors() {
        var successes = 0
        repeat(CHECKS_PER_TEST) {
            if (testScenario(false)) {
                successes++
            }
        }
        val percentage = (successes.toDouble() / CHECKS_PER_TEST * 100)
        println("Testing (with NO errors in distance measurements): $successes/$CHECKS_PER_TEST passed (${percentage}%)")
        assertTrue("Found location (within tolerance of ${ACCEPTABLE_TOLERANCE}) ${successes} out of ${CHECKS_PER_TEST} times", successes == CHECKS_PER_TEST)
    }

    @Test
    fun test3DMultilaterationWithErrors() {
        var successes = 0
        repeat(CHECKS_PER_TEST) {
            if (testScenario(true)) {
                successes++
            }
        }
        val percentage = (successes.toDouble() / CHECKS_PER_TEST * 100)
        println("Testing (with errors in distance measurements): $successes/$CHECKS_PER_TEST passed (${percentage}%)")
        assertTrue("Found location (within tolerance of ${ACCEPTABLE_TOLERANCE}) ${successes} out of ${CHECKS_PER_TEST} times", successes >= (1 - ACCEPTABLE_ERROR_RATE) * CHECKS_PER_TEST)
    }

    fun dist(p1: DoubleArray, p2: DoubleArray): Double {
        require(p1.size == p2.size) { "Points must have same dimension, p1: ${p1.size} and p2: ${p2.size}" }
        return sqrt(p1.indices.sumOf { (p2[it] - p1[it]).pow(2) })
    }

    private fun testScenario(error: Boolean): Boolean {
        val userTrue = doubleArrayOf(Random.nextDouble(0.0, USER_LOCATION_SQUARE), Random.nextDouble(0.0, USER_LOCATION_SQUARE), 1.0)
        val userFound = testSolver(userTrue, error, MAX_NUM_BEACONS)
        val d = dist(userTrue, userFound.copyOfRange(0, 3))

        // assertTrue("Found location (${userFound.joinToString(separator = ", ")}) is too far (distance = ${d}, tolerance = ${ACCEPTABLE_TOLERANCE}) from actual location (${userTrue.joinToString(separator = ", ")})", d <= ACCEPTABLE_TOLERANCE)
        return d <= ACCEPTABLE_TOLERANCE
    }

    private fun testSolver(origin: DoubleArray, error: Boolean, numCoords: Int) : DoubleArray {
        val distances = DoubleArray(numCoords)
        val prev = DoubleArray(3)
        prev[0] = origin[0] + DISTANCE_FROM_PREV*Random.nextDouble(-1.0, 1.0)
        prev[1] = origin[1] + DISTANCE_FROM_PREV*Random.nextDouble(-1.0, 1.0)
        prev[2] = origin[2] + DISTANCE_FROM_PREV*Random.nextDouble(-1.0, 1.0)

        for (i in 0 until numCoords) {
            var d = dist(coordinates[i], origin)

            if (error) {
                d = getRandomFromActualDistance(d)
            }

            distances[i] = d
        }

        return getLocation(prev, coordinates, distances)
    }

    // If implementation changes, change this
    private fun getLocation(prev: DoubleArray, coordinates: Array<DoubleArray>, distances: DoubleArray) : DoubleArray {
        val trilaterationFunction = TrilaterationFunction(prev, coordinates, distances)

        return trilaterationFunction.solve()
    }

    private fun getRandomRSSI(actual: Double) : Double {
        return actual + sqrt(RSSISampleSize) * tdist.sample() / sqrt(RSSIVariance)
    }

    private fun getRSSIFromDistance(distance: Double) : Double {
        return -65.0 - log10(distance) * 10 * 4
    }

    private fun getDistanceFromRSSI(rssi: Double) : Double {
        return 10.0.pow((-65.0 - rssi) / (10 * 4).toDouble())
    }

    private fun getRandomFromActualDistance(actual: Double) : Double {
        val actualRSSI = getRSSIFromDistance(actual)
        val randomRSSI = getRandomRSSI(actualRSSI)
        return getDistanceFromRSSI(randomRSSI)
    }
}
