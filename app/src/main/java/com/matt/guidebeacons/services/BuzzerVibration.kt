package com.matt.guidebeacons.services

import android.Manifest
import android.bluetooth.le.ScanResult
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.Vibrator
import android.os.VibratorManager
import android.widget.Toast
import androidx.core.content.ContextCompat
import com.matt.guidebeacons.beacons.BeaconData

/**
 * temp? should probably be calibrated per buzzer beacon?
 * @see[com.matt.guidebeacons.beacons.Beacon.buzzerSensitivity]
 */
const val NEARBY_BUZZER_RSSI = -55

class BuzzerVibration {
    private val appContext: Context

    private val vibrator: Vibrator

    private val beaconProjects = BeaconData.getBeaconProjects()

    private var isToastShowing = false

    constructor(appContext: Context) {
        this.appContext = appContext
        this.vibrator = initializeVibrator()
    }

    private fun initializeVibrator(): Vibrator {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager = appContext.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
            vibratorManager.defaultVibrator
        }
        else {
            @Suppress("DEPRECATION")
            appContext.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }
    }

    fun buzzForNearbyDevice(result: ScanResult) {
        return
        if (!isToastShowing) {
            Toast.makeText(
                appContext,
                "Close to ${beaconProjects[result.device.address] ?: "Unknown Beacon"}",
                Toast.LENGTH_SHORT
            ).show()
            isToastShowing = true

            Handler(Looper.getMainLooper()).postDelayed({
                isToastShowing = false
            }, Toast.LENGTH_SHORT.toLong())

            if (ContextCompat.checkSelfPermission(
                    appContext,
                    Manifest.permission.VIBRATE
                ) == PackageManager.PERMISSION_GRANTED
            ) {
                vibrator.vibrate(500)
            }
        }
    }
}