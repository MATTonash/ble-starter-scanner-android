package com.punchthrough.blestarterappandroid

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.Context
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import com.matt.guidebeacons.beacons.BeaconData
import com.matt.guidebeacons.utils.readableBleScanFailedErrorCode
import com.punchthrough.blestarterappandroid.ble.ConnectionManager
import timber.log.Timber


/**
 * This is a worker class that continually scans across activities, previously we relied on data
 * from scanning in MainActivity so this might look a bit rough
 *
 * More on bluetooth in the doc
 */
class BluetoothWorkerClass private constructor() {
    private var scanResults = mutableListOf<ScanResult>()
    private var isScanning = false
    private lateinit var bluetoothAdapter: BluetoothAdapter
    private var bleScanner: android.bluetooth.le.BluetoothLeScanner? = null
    private var scanCallback: ((List<ScanResult>) -> Unit)? = null
    private lateinit var appContext: Context

    private val handler = Handler(Looper.getMainLooper())

    private val beaconProjects = BeaconData.getBeaconProjects()


    // Makes sure this class is only instantiated once
    // Separate from and independent to any other class (not like an activity)
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
        if (bluetoothManager.adapter == null) {
            Toast.makeText(appContext, "Bluetooth not supported on this device", Toast.LENGTH_LONG).show()
            return
        }
        bluetoothAdapter = bluetoothManager.adapter

        if (!bluetoothAdapter.isEnabled) {
            Toast.makeText(appContext, "Bluetooth is disabled. Please enable Bluetooth.", Toast.LENGTH_LONG).show()
            // Leave bleScanner null and avoid calling it elsewhere until Bluetooth is enabled.
            bleScanner = null
            return
        }

        bleScanner = bluetoothAdapter.bluetoothLeScanner
    }

    private val scanSettings = ScanSettings.Builder()
        .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY) // Changed to LOW_POWER mode
        .build()

    private val scanRunnable = object : Runnable {
        @SuppressLint("MissingPermission")
        override fun run() {
            if (isScanning) {

                // Stop scanning
                bleScanner?.stopScan(bleScanCallback)
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

    /**
     * Checks if a certain beacon (based of MAC Address) is in the scan list
     * @param MACAddress String of address
     */
    fun caughtInScan(MACAddress: String): ScanResult? {
        for (scanResult in getCurrentResults()) {
            if (scanResult.device.address == MACAddress) {
                return scanResult
            }
        }
        return null
    }

    // WHAT DOES THIS MEANNNNNNN (actually doesn't matter but would be nice to find out eventually...)
    @SuppressLint("MissingPermission")
    private fun startScanCycle() {
        if (!::bluetoothAdapter.isInitialized || !::appContext.isInitialized) {
            Timber.e("BluetoothWorkerClass not initialized")
            return
        }

        if (appContext.hasRequiredRuntimePermissions()) {
            bleScanner?.startScan(null, scanSettings, bleScanCallback)
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
        if (isScanning && !::bluetoothAdapter.isInitialized) {
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
        bleScanner?.stopScan(bleScanCallback)
        isScanning = false
        continuousScanning = false

        for (beacon in beaconProjects.values) {
            beacon.resetKalmanFilter()
        }

        Timber.d("Stopped BLE scan")
    }

    fun isScanning(): Boolean = isScanning

    fun getCurrentResults(): List<ScanResult> = scanResults.toList()


    private val bleScanCallback = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            if (result.device.address in beaconProjects) {
                val indexQuery =
                    scanResults.indexOfFirst { it.device.address == result.device.address }
                if (indexQuery != -1) {
                    scanResults[indexQuery] = result
                } else {
                    scanResults.add(result)
                }
            }

            // Sort results by RSSI
            scanResults.sortByDescending { it.rssi }

            // Notify callback on main thread
            handler.post {
                scanCallback?.invoke(scanResults.toList())
            }
        }

        override fun onScanFailed(errorCode: Int) {
            Timber.e("BLE scan failed with code: ${readableBleScanFailedErrorCode(errorCode)}")
            isScanning = false
        }
    }
}