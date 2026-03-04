package com.matt.guidebeacons.beacons

import kotlinx.serialization.Serializable
import kotlin.collections.plusAssign
import kotlin.div
import kotlin.math.pow
import kotlin.times

/**
 * We decided to implement a Beacon class that will store and manage all the relevant mapping data and methods
 * just so that it can be used more simply across the activities
 * Previously we were relying on referencing MainActivity
 */
@Serializable(with = BeaconSerializer::class)
class Beacon(beaconName: String,
             calibrationRSSI: Int,
             x: Double,
             y: Double,
             z: Double
) {
    private var beaconName = beaconName
    private var calibrationRSSI = calibrationRSSI
    private var coordinates = doubleArrayOf(x, y, z)
    private var buzzerSensitivity = 0
    private var beaconType = BeaconType.DEFAULT

    // Kalman filter variables
    private var filteredRSSI: Double = calibrationRSSI.toDouble() // Arbitrary value
    private var estimateError: Double = 1.0  // P - estimate uncertainty
    private val processNoise: Double = 0.01  // Q - how much we expect RSSI to change
    private val measurementNoise: Double = 4.0  // R - sensor noise (tune based on RSSI variance)
    private var isInitialized: Boolean = false

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
        return "(${coordinates[0]}, ${coordinates[1]}, ${coordinates[2]})"
    }

    public fun getBuzzerSensitivity(): Int {
        return buzzerSensitivity
    }

    public fun getBeaconType(): BeaconType {
        return beaconType
    }

    public fun setBuzzerSensitivity(sensitivity: Int) {
        buzzerSensitivity = sensitivity
    }

    public fun setBeaconType(type: BeaconType) {
        beaconType = type
    }

    public override fun toString(): String {
        return beaconName;
    }

    public fun updateData(beaconName: String, calibrationRSSI: Int, x: Double, y: Double, z: Double) {
        this.beaconName = beaconName
        this.calibrationRSSI = calibrationRSSI
        this.coordinates = doubleArrayOf(x, y, z)
    }

    public fun updateFilteredRSSI(measuredRSSI: Int): Double {
        if (!isInitialized) {
            filteredRSSI = measuredRSSI.toDouble()
            isInitialized = true
            return filteredRSSI
        }

        // Prediction step
        val predictedError = estimateError + processNoise

        // Update step
        val kalmanGain = predictedError / (predictedError + measurementNoise)
        filteredRSSI += kalmanGain * (measuredRSSI - filteredRSSI)
        estimateError = (1 - kalmanGain) * predictedError

        return filteredRSSI
    }

    public fun getFilteredRSSI(): Double {
        return filteredRSSI
    }

    public fun resetKalmanFilter() {
        this.filteredRSSI = calibrationRSSI.toDouble()
        this.isInitialized = false
    }
}