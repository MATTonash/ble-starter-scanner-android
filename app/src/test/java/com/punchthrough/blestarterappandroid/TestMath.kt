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

import kotlin.random.Random

import kotlin.math.sqrt
import kotlin.math.pow


/**
 * Local unit tests, which will execute on the development machine (host).
 *
 */

private const val ACCEPTABLE_TOLERANCE = 0.1 // in m
private const val CHECKS_PER_TEST = 100000

private const val NUM_BEACONS = 3

private const val BEACON_ERROR = 0.1 // as a percentage (0.2 means up to 20% off actual distance)
private const val USER_LOCATION_SQUARE = 5.0 // in m
// the user's coordinates can be (x,y) where |x|,|y| < USER_LOCATION_SQUARE




class TestMath {
    @Test
    fun test2DMultilaterationWithNoErrors() {
        var successes = 0
        repeat(CHECKS_PER_TEST) {
            if (testScenario(0.0)) {
                successes++
            }
        }
        assertTrue("Found location (within tolerance of ${ACCEPTABLE_TOLERANCE}) ${successes} out of ${CHECKS_PER_TEST} times", successes == CHECKS_PER_TEST)
    }

    @Test
    fun test2DMultilaterationWithErrors() {
        var successes = 0
        repeat(CHECKS_PER_TEST) {
            if (testScenario(BEACON_ERROR)) {
                successes++
            }
        }
    }

    fun dist(p1: DoubleArray, p2: DoubleArray): Double {
        require(p1.size == p2.size) { "Points must have same dimension" }
        return sqrt(p1.indices.sumOf { (p2[it] - p1[it]).pow(2) })
    }

    private fun testScenario(error: Double): Boolean {
        val userTrue = doubleArrayOf(Random.nextDouble(-USER_LOCATION_SQUARE, USER_LOCATION_SQUARE), Random.nextDouble(-USER_LOCATION_SQUARE, USER_LOCATION_SQUARE))
        val userFound = testSolver(userTrue, error, NUM_BEACONS).copyOfRange(0, 2)
        val d = dist(userTrue, userFound)

        // assertTrue("Found location (${userFound.joinToString(separator = ", ")}) is too far (distance = ${d}, tolerance = ${ACCEPTABLE_TOLERANCE}) from actual location (${userTrue.joinToString(separator = ", ")})", d <= ACCEPTABLE_TOLERANCE)
        return d <= ACCEPTABLE_TOLERANCE
    }

    private fun testSolver(origin: DoubleArray, error: Double, numCoords: Int) : DoubleArray {
        val coords = Array(numCoords) { DoubleArray(0) }
        val distances = DoubleArray(numCoords)


        val x = 2;
        coords[0] = doubleArrayOf(USER_LOCATION_SQUARE / x, USER_LOCATION_SQUARE / x)
        coords[1] = doubleArrayOf(-USER_LOCATION_SQUARE / x, USER_LOCATION_SQUARE / x)
        coords[2] = doubleArrayOf(USER_LOCATION_SQUARE / x, -USER_LOCATION_SQUARE / x)


        for (i in 0 until numCoords) {
            var d = dist(coords[i], origin)
            if (error > 0) {
                d = d * Random.nextDouble(1 - error, 1 + error)
            }

            distances[i] = d
        }

        return getLocation(coords, distances)
    }

    // If implementation changes, change this
    private fun getLocation(coords: Array<DoubleArray>, distances: DoubleArray) : DoubleArray {
        val trilaterationFunction = TrilaterationFunction(coords)
        trilaterationFunction.setBeaconDistances(distances)

        return trilaterationFunction.solve()
    }
}
