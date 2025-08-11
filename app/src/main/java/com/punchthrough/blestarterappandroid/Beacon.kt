/*
 * Copyright 2025 Punch Through Design LLC
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

import kotlin.math.pow

class Beacon(beaconName: String,
             callibrationRSSI: Int,
             x: Double,
             y: Double) {
    private val beaconName = beaconName;
    private val callibrationRSSI = callibrationRSSI
    private val coordinates = arrayOf(x, y)
    private var buzzerSensitivity = 0;

    public fun calculateDistance(rssi: Int, txPower: Double): Double{
        return 10.0.pow((callibrationRSSI - rssi)/(10*txPower))
    }

    public fun getCoordinates(): Array<Double>{
        return coordinates
    }

    public fun setBuzzerSensitivity(sensitivity: Int){
        buzzerSensitivity = sensitivity
    }

    public fun getBuzzerSensitivity(): Int{
        return buzzerSensitivity
    }

    public override fun toString(): String {
        return beaconName;
    }
}