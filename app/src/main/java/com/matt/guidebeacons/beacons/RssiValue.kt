package com.matt.guidebeacons.beacons

import kotlinx.serialization.Serializable
import java.util.Calendar

@OptIn(kotlinx.serialization.InternalSerializationApi::class)
@Serializable
class RssiValue(
    private val measuredRssi: Double,
    private val measuredDistance: Double,
    private val timestamp: String) {

    constructor(measuredRssi: Double, measuredDistance: Double)
        : this(measuredRssi, measuredDistance, Calendar.getInstance().time.toString())

    fun getMeasuredRssi() : Double {
        return measuredRssi
    }

    fun getMeasuredDistance() : Double {
        return measuredDistance
    }

    /**
     * String representation of the time the RSSI value was captured, for potential sorting/tracking
     */
    fun getTimestamp() : String {
        return timestamp
    }
}