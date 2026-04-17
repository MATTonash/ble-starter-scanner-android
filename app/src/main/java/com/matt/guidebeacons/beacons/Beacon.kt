package com.matt.guidebeacons.beacons

import android.content.Context
import com.punchthrough.blestarterappandroid.DistanceRegression
import kotlinx.serialization.Serializable
import kotlin.math.ln
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
             y: Double,
             z: Double
) {
    private var beaconName = beaconName
    private var calibrationRSSI = calibrationRSSI
    private var coordinates = doubleArrayOf(x, y, z)
    private var buzzerSensitivity = 0
    private var beaconType = BeaconType.DEFAULT

    private lateinit var regressionFunction: DistanceRegression

    // Kalman filter variables
    private var filteredRSSI: Double = calibrationRSSI.toDouble() // Arbitrary value
    private var estimateError: Double = 1.0  // P - estimate uncertainty
    private val processNoise: Double = 0.01  // Q - how much we expect RSSI to change
    private val measurementNoise: Double = 4.0  // R - sensor noise (tune based on RSSI variance)
    private var isInitialized: Boolean = false

    public fun calculateDistance(rssi: Int, txPower: Int, context: Context): Double {
        val rssiCollection = RssiCollection.readFromFile(
            context,
            BeaconData.getBeaconMacAddress(this).toString(),
            this.beaconName
        )

        val measurements = rssiCollection.getMeasurements().filter { it.getType() == RssiValue.CollectionType.AVERAGE }
        val yVal = DoubleArray(measurements.size)
        val xVal = DoubleArray(measurements.size)

        val oneMetreRssi = measurements.find { it.getMeasuredDistance() == 1.0}
        if (oneMetreRssi != null) {
            calibrationRSSI = oneMetreRssi.getMeasuredRssi().toInt()
        }

        if (measurements.size > 4) {
            for ((i, rssiVal) in measurements.withIndex()) {
                yVal[i] = rssiVal.getMeasuredRssi() // it sooooo has to do with my indexing fuckkk
                xVal[i] = rssiVal.getMeasuredDistance()
            }
            regressionFunction = DistanceRegression(yVal, xVal)

            val regCoeff = regressionFunction.coefficients
            val nonNegRssi = -rssi.toDouble()
//            val distance = regCoeff[0] * (nonNegRssi.pow(regCoeff[1]))
            val distance = regCoeff[0]+ regCoeff[1]* ln(nonNegRssi)
            return distance
        }
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