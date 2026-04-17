package com.matt.guidebeacons.beacons

import kotlinx.serialization.Serializable
import kotlin.math.pow

/**
 * We decided to implement a Beacon class that will store and manage all the relevant mapping data and methods
 * just so that it can be used more simply across the activities
 * Previously we were relying on referencing MainActivity
 */
@Serializable(with = BeaconSerializer::class)
class Beacon(private var beaconName: String,
             private var calibrationRSSI: Int,
             x: Double,
             y: Double,
             z: Double
) {
    private var coordinates = doubleArrayOf(x, y, z)
    private var buzzerSensitivity = 0
    private var beaconType = BeaconType.DEFAULT

    // Kalman filter variables
    private var filteredRSSI: Double = calibrationRSSI.toDouble() // Arbitrary value
    private var estimateError: Double = 50.0  // P - estimate uncertainty
    private val processNoise: Double = 0.5  // Q - how much we expect RSSI to change
    private val measurementNoise: Double = 25.0  // R - sensor noise (tune based on RSSI variance)
    private var isInitialized: Boolean = false

    fun calculateDistance(rssi: Int, txPower: Int): Double{
        return 10.0.pow((calibrationRSSI - rssi).toDouble()/(10*txPower).toDouble())
    }

    fun getCalibrationRSSI(): Int {
        return calibrationRSSI
    }

    fun getCoordinates(): DoubleArray {
        return coordinates
    }

    fun getCoordinatesString(): String {
        // bit gross to hard-code indices, is there a better way?
        return "(${coordinates[0]}, ${coordinates[1]}, ${coordinates[2]})"
    }

    fun getBuzzerSensitivity(): Int {
        return buzzerSensitivity
    }

    fun getBeaconType(): BeaconType {
        return beaconType
    }

    fun setBuzzerSensitivity(sensitivity: Int) {
        buzzerSensitivity = sensitivity
    }

    fun setBeaconType(type: BeaconType) {
        beaconType = type
    }

    override fun toString(): String {
        return beaconName
    }

    fun updateData(beaconName: String, calibrationRSSI: Int, x: Double, y: Double, z: Double) {
        this.beaconName = beaconName
        this.calibrationRSSI = calibrationRSSI
        this.coordinates = doubleArrayOf(x, y, z)
    }

    fun updateFilteredRSSI(measuredRSSI: Int): Double {
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

    fun getFilteredRSSI(): Double {
        return filteredRSSI
    }

    fun resetKalmanFilter() {
        this.filteredRSSI = calibrationRSSI.toDouble()
        this.isInitialized = false
    }
}