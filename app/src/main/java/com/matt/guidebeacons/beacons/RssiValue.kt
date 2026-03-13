package com.matt.guidebeacons.beacons

import kotlinx.serialization.Serializable
import java.time.Clock
import java.util.Calendar
import java.util.Date
import kotlin.time.TimeMark
import kotlin.time.TimeSource

@OptIn(kotlinx.serialization.InternalSerializationApi::class)
@Serializable
class RssiValue(
    private val measuredRssi: Double,
    private val measuredDistance: Double,
    private val timestamp: String) {

    constructor(measuredRssi: Double, measuredDistance: Double)
        : this(measuredRssi, measuredDistance, Calendar.getInstance().time.toString())
}