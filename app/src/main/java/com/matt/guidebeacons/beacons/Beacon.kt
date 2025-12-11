package com.matt.guidebeacons.beacons

import kotlinx.serialization.Serializable
import kotlin.math.pow

/**
 * We decided to implement a Beacon class that will store and manage all the relevant mapping data and methods
 * just so that it can be used more simply across the activities
 * Previously we were relying on referencing MainActivity
 */
@Serializable(with = BeaconSerializer::class)
class Beacon(beaconName: String,
             calibrationRSSI: Int,
             x: Double,
             y: Double) {
    private var beaconName = beaconName;
    private var calibrationRSSI = calibrationRSSI
    private var coordinates = doubleArrayOf(x, y)
    private var buzzerSensitivity = 0;

    public fun calculateDistance(rssi: Int, txPower: Int): Double{
        return 10.0.pow((calibrationRSSI - rssi).toDouble()/(10*txPower).toDouble())
    }

    public fun getCalibrationRSSI(): Int {
        return calibrationRSSI
    }

    public fun getCoordinates(): DoubleArray {
        return coordinates
    }

    public fun getCoordinatesString(): String {
        // bit gross to hard-code indices, is there a better way?
        return "(${coordinates[0]}, ${coordinates[1]})"
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

    public fun updateData(beaconName: String, calibrationRSSI: Int, x: Double, y: Double) {
        this.beaconName = beaconName
        this.calibrationRSSI = calibrationRSSI
        this.coordinates = doubleArrayOf(x, y)
    }
}