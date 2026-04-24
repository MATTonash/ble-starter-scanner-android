package com.matt.guidebeacons.utils

import android.bluetooth.le.ScanCallback

private val macRegex = Regex("([0-9a-fA-F]{2}:){5}[0-9a-fA-F]{2}")
fun isMacAddress(str: String) = str.matches(macRegex)

fun readableBleScanFailedErrorCode(errorCode: Int): String {
    return when (errorCode) {
        ScanCallback.SCAN_FAILED_ALREADY_STARTED -> "${errorCode} (SCAN_FAILED_ALREADY_STARTED)"
        ScanCallback.SCAN_FAILED_APPLICATION_REGISTRATION_FAILED -> "${errorCode} (SCAN_FAILED_APPLICATION_REGISTRATION_FAILED)"
        ScanCallback.SCAN_FAILED_FEATURE_UNSUPPORTED -> "${errorCode} (SCAN_FAILED_FEATURE_UNSUPPORTED)"
        ScanCallback.SCAN_FAILED_INTERNAL_ERROR -> "${errorCode} (SCAN_FAILED_INTERNAL_ERROR)"
        ScanCallback.SCAN_FAILED_OUT_OF_HARDWARE_RESOURCES -> "${errorCode} (SCAN_FAILED_OUT_OF_HARDWARE_RESOURCES)"
        ScanCallback.SCAN_FAILED_SCANNING_TOO_FREQUENTLY -> "${errorCode} (SCAN_FAILED_SCANNING_TOO_FREQUENTLY)"
        else -> errorCode.toString()
    }
}
