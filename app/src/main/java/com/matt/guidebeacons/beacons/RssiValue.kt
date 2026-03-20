package com.matt.guidebeacons.beacons

import kotlinx.serialization.Serializable
import java.util.Calendar

@OptIn(kotlinx.serialization.InternalSerializationApi::class)
@Serializable
class RssiValue(
    private val measuredRssi: Double,
    private val measuredDistance: Double,
    private val timestamp: String,
    private val type: CollectionType) {

    constructor(measuredRssi: Double, measuredDistance: Double, type: CollectionType)
        : this(measuredRssi, measuredDistance, Calendar.getInstance().time.toString(), type)

    fun getMeasuredRssi() : Double {
        return measuredRssi
    }

    fun getMeasuredDistance() : Double {
        return measuredDistance
    }

    fun getType() : CollectionType {
        return type
    }

    /**
     * String representation of the time the RSSI value was captured, for potential sorting/tracking
     * @see[java.util.Date]
     */
    fun getTimestamp() : String {
        return timestamp
    }

    enum class CollectionType {
        SNAPSHOT,
        RECORDING,
        AVERAGE
    }
}