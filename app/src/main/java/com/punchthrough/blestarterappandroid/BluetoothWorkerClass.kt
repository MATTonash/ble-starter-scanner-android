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

        fun getInstance(): BluetoothWorkerClass {
            return instance ?: synchronized(this) {
                instance ?: BluetoothWorkerClass().also { instance = it }
            }
        }
    }

    fun initialize(context: Context) {
        appContext = context.applicationContext
        val bluetoothManager = appContext.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        bluetoothAdapter = bluetoothManager.adapter
        bleScanner = bluetoothAdapter.bluetoothLeScanner
    }

    private val scanSettings = ScanSettings.Builder()
        .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
        .build()

    @SuppressLint("MissingPermission")
    fun startScanning(callback: (List<ScanResult>) -> Unit) {
        if (!::bluetoothAdapter.isInitialized || !::appContext.isInitialized) {
            Timber.e("BluetoothWorkerClass not initialized")
            return
        }

        if (isScanning) {
            Timber.e("Already scanning")
            return
        }

        scanCallback = callback
        scanResults.clear()

        if (appContext.hasRequiredBluetoothPermissions()) {  // Now using the context extension function
            bleScanner.startScan(null, scanSettings, bleScanCallback)
            isScanning = true
            Timber.d("Started BLE scan")
        } else {
            Timber.e("Missing required Bluetooth permissions")
        }
    }

    @SuppressLint("MissingPermission")
    fun stopScanning() {
        if (!isScanning || !::bluetoothAdapter.isInitialized) return

        bleScanner.stopScan(bleScanCallback)
        isScanning = false
        Timber.d("Stopped BLE scan")
    }

    fun isScanning(): Boolean = isScanning

    fun getCurrentResults(): List<ScanResult> = scanResults.toList()

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