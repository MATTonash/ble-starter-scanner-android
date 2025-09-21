package com.matt.guidebeacons.beacons

import kotlin.math.pow

/**
 * We decided to implement a Beacon class that will store and manage all the relevant mapping data and methods
 * just so that it can be used more simply across the activities
 * Previously we were relying on referencing MainActivity
 */
class Beacon(beaconName: String,
             calibrationRSSI: Int,
             x: Double,
             y: Double) {
    private val beaconName = beaconName;
    private val calibrationRSSI = calibrationRSSI
    private val coordinates = doubleArrayOf(x, y)
    private var buzzerSensitivity = 0;

    public fun calculateDistance(rssi: Int, txPower: Int): Double{
        return 10.0.pow((calibrationRSSI - rssi).toDouble()/(10*txPower).toDouble())
    }

    public fun getCoordinates(): DoubleArray{
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