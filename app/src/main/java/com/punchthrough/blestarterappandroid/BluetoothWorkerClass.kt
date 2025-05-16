package com.punchthrough.blestarterappandroid

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.Context
import android.os.Handler
import android.os.Looper
import timber.log.Timber
import kotlin.math.pow


class BluetoothWorkerClass private constructor() {
    private var scanResults = mutableListOf<ScanResult>()
    private var isScanning = false
    private lateinit var bluetoothAdapter: BluetoothAdapter
    private lateinit var bleScanner: android.bluetooth.le.BluetoothLeScanner
    private var scanCallback: ((List<ScanResult>) -> Unit)? = null
    private lateinit var appContext: Context

    private val handler = Handler(Looper.getMainLooper())



    companion object {
        @Volatile
        private var instance: BluetoothWorkerClass? = null

        // Default scan parameters
        private const val SCAN_PERIOD = 5000L // Scan for 5 seconds
        private const val SCAN_INTERVAL = 10000L // Wait 10 seconds between scans

        fun getInstance(): BluetoothWorkerClass {
            return instance ?: synchronized(this) {
                instance ?: BluetoothWorkerClass().also { instance = it }
            }
        }
    }

    private var scanPeriod: Long = SCAN_PERIOD
    private var scanInterval: Long = SCAN_INTERVAL
    private var continuousScanning = false

    fun initialize(context: Context) {
        appContext = context.applicationContext
        val bluetoothManager = appContext.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        bluetoothAdapter = bluetoothManager.adapter
        bleScanner = bluetoothAdapter.bluetoothLeScanner
    }

    private val scanSettings = ScanSettings.Builder()
        .setScanMode(ScanSettings.SCAN_MODE_LOW_POWER) // Changed to LOW_POWER mode
        .build()

    private val scanRunnable = object : Runnable {
        @SuppressLint("MissingPermission")
        override fun run() {
            if (isScanning) {
                // Stop scanning
                bleScanner.stopScan(bleScanCallback)
                isScanning = false
                Timber.d("Stopped BLE scan")

                if (continuousScanning) {
                    // Schedule next scan after interval
                    handler.postDelayed({
                        startScanCycle()
                    }, scanInterval)
                }
            } else {
                // Start scanning
                startScanCycle()
            }
        }
    }

    @SuppressLint("MissingPermission")
    private fun startScanCycle() {
        if (!::bluetoothAdapter.isInitialized || !::appContext.isInitialized) {
            Timber.e("BluetoothWorkerClass not initialized")
            return
        }

        if (appContext.hasRequiredBluetoothPermissions()) {
            bleScanner.startScan(null, scanSettings, bleScanCallback)
            isScanning = true
            Timber.d("Started BLE scan")

            // Schedule scan stop after scanPeriod
            handler.postDelayed(scanRunnable, scanPeriod)
        } else {
            Timber.e("Missing required Bluetooth permissions")
        }
    }

    @SuppressLint("MissingPermission")
    fun startScanning(
        callback: (List<ScanResult>) -> Unit,
        continuous: Boolean = true,
        period: Long = SCAN_PERIOD,
        interval: Long = SCAN_INTERVAL
    ) {
        if (isScanning) {
            Timber.e("Already scanning")
            return
        }

        scanCallback = callback
        scanResults.clear()
        continuousScanning = continuous
        scanPeriod = period
        scanInterval = interval

        startScanCycle()
    }

    @SuppressLint("MissingPermission")
    fun stopScanning() {
        if (!isScanning || !::bluetoothAdapter.isInitialized) return

        handler.removeCallbacks(scanRunnable)
        bleScanner.stopScan(bleScanCallback)
        isScanning = false
        continuousScanning = false
        Timber.d("Stopped BLE scan")
    }

    fun isScanning(): Boolean = isScanning

    fun getCurrentResults(): List<ScanResult> = scanResults.toList()

    fun rssiToDistance(rssi: Int): Double {
        // need to calibrate beacons
        val calibrationRSSI = -68
        val txPower = 2.3
        return 10.0.pow((calibrationRSSI - rssi)/(10*txPower))
    }

    private val bleScanCallback = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            val indexQuery = scanResults.indexOfFirst { it.device.address == result.device.address }
            if (indexQuery != -1) {
                scanResults[indexQuery] = result
            } else {
                scanResults.add(result)
            }

            // Sort results by RSSI
            scanResults.sortByDescending { it.rssi }

            // Notify callback on main thread
            handler.post {
                scanCallback?.invoke(scanResults.toList())
            }
        }

        override fun onScanFailed(errorCode: Int) {
            Timber.e("BLE Scan Failed with code $errorCode")
            isScanning = false
        }
    }
}