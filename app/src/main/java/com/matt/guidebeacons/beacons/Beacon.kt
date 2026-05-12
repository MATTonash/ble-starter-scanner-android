package com.matt.guidebeacons.beacons

import android.content.Context
import com.punchthrough.blestarterappandroid.DistanceRegression
import kotlinx.serialization.Serializable
import kotlin.math.log10
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

    private var regressionFunction: DistanceRegression? = null

    // Kalman filter variables
    private var filteredRSSI: Double = calibrationRSSI.toDouble() // Arbitrary value
    private var estimateError: Double = 50.0  // P - estimate uncertainty
    private val processNoise: Double = 0.5  // Q - how much we expect RSSI to change
    private val measurementNoise: Double = 20.0  // R - sensor noise (tune based on RSSI variance)
    private var isInitialized: Boolean = false

    public fun calculateDistance(rssi: Int, txPower: Int, context: Context): Double {
        if (regressionFunction === null) {
            generateRegressionFunction(rssi, txPower, context)
        }

        if (regressionFunction !== null) {
            val regCoeff = regressionFunction!!.coefficients
            val nonNegRssi = -rssi.toDouble()
//            val distance = regCoeff[0] * (nonNegRssi.pow(regCoeff[1]))
//            val distance = regCoeff[0] * exp(nonNegRssi * regCoeff[1])
            val distance = regCoeff[0] + regCoeff[1] * log10(nonNegRssi)
            return distance
        }
        return calculateDistanceGeneric(rssi, txPower)
    }

    public fun calculateDistanceGeneric(rssi: Int, txPower: Int): Double {
        return 10.0.pow((calibrationRSSI - rssi).toDouble() / (10 * txPower).toDouble())
    }

    /**
     * Copied function body; only use for debugging/comparing.
     * @see[calculateDistance]
     */
    public fun calculateDistanceUsingRegression(rssi: Int, txPower: Int, context: Context): Double {
        if (regressionFunction === null) {
            generateRegressionFunction(rssi, txPower, context)
        }

        if (regressionFunction !== null) {
            val regCoeff = regressionFunction!!.coefficients
            val nonNegRssi = -rssi.toDouble()
//            val distance = regCoeff[0] * (nonNegRssi.pow(regCoeff[1]))
//            val distance = regCoeff[0] * exp(nonNegRssi * regCoeff[1])
            val distance = regCoeff[0]* nonNegRssi.pow(regCoeff[1])
            return distance
        }

        return -1.0;
    }

    /**
     * Attempts to generate a [DistanceRegression] for this beacon,
     * based on collected RSSI values.
     * @return whether or not a regression function was successfully generated.
     */
    public fun generateRegressionFunction(rssi: Int, txPower: Int, context: Context): Boolean {
        val rssiCollection = RssiCollection.readFromFile(
            context,
            BeaconData.getBeaconMacAddress(this).toString(),
            this.beaconName
        )

        val measurements = rssiCollection.getMeasurements().filter { it.getType() == RssiValue.CollectionType.AVERAGE }
        val yVal = DoubleArray(measurements.size)
        val xVal = DoubleArray(measurements.size)

        val oneMetreRssi = measurements.find { it.getMeasuredDistance() == 1.0 }
        if (oneMetreRssi != null) {
            calibrationRSSI = oneMetreRssi.getMeasuredRssi().toInt()
        }

        if (measurements.size > 4) {
            for ((i, rssiVal) in measurements.withIndex()) {
                yVal[i] = rssiVal.getMeasuredRssi() // it sooooo has to do with my indexing fuckkk
                xVal[i] = rssiVal.getMeasuredDistance()
            }
            regressionFunction = DistanceRegression(xVal, yVal)
            return true
        }

        return false
    }

    public fun getRegressionCoefficients(): DoubleArray? {
        return regressionFunction?.coefficients
    }

    public fun clearRegression() {
        regressionFunction = null
    }

    public fun getCalibrationRSSI(): Int {
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