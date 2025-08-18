package com.punchthrough.blestarterappandroid

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.Vibrator
import android.os.VibratorManager
import android.widget.Toast
import androidx.core.content.ContextCompat
import com.punchthrough.blestarterappandroid.ble.ConnectionManager
import timber.log.Timber


class BluetoothWorkerClass private constructor() {
    private var scanResults = mutableListOf<ScanResult>()
    private var isScanning = false
    private lateinit var bluetoothAdapter: BluetoothAdapter
    private lateinit var bleScanner: android.bluetooth.le.BluetoothLeScanner
    private var scanCallback: ((List<ScanResult>) -> Unit)? = null
    private lateinit var appContext: Context
    private val connectedDevices = mutableSetOf<String>() // Track connected devices
    private val connectionCheckHandler = Handler(Looper.getMainLooper())
    private val connectionCheckInterval = 5000L // Check connections every 5 seconds
    private val maxConnections = 30 // Maximum number of simultaneous connections

    private val handler = Handler(Looper.getMainLooper())

    private val beaconProjects = mapOf(
        "80:EC:CC:CD:33:28" to Beacon("Losing THings", -60, 0.0, 1.0),
        "80:EC:CC:CD:33:7C" to Beacon("Happy Mornings", -57, 1.0, 2.0),
        "80:EC:CC:CD:33:7E" to Beacon("STEM", -59, 2.0, 2.0),
        "80:EC:CC:CD:33:58" to Beacon("Visual Clutter", -60, 2.0, 1.0),
        "00:3C:84:28:87:01" to Beacon("MAP", -58, 1.0, 0.0),
        "00:3C:84:28:77:AB" to Beacon("Dance", -60, 1.00, 1.0),
    )

    private lateinit var vibrator: Vibrator
    private var isToastShowing = false

    public fun getBeaconProjects():Map<String, Beacon>{
        return beaconProjects;
    }


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

    /**
     * Initialises the companion object according to the activity
     * @param context of the environment (typically activity)
     */
    fun initialize(context: Context) {
        appContext = context.applicationContext
        val bluetoothManager = appContext.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        bluetoothAdapter = bluetoothManager.adapter
        bleScanner = bluetoothAdapter.bluetoothLeScanner

        initializeVibrator()
    }

    private val scanSettings = ScanSettings.Builder()
        .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY) // Changed to LOW_POWER mode
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

    /**
     * Checks if a certain beacon (based of MAC Address) is in the scan list
     * @param MACAddress String of address
     */
    fun caughtInScan(MACAddress : String) : ScanResult? {
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

    private val connectionCheckRunnable = object : Runnable {
        override fun run() {
            //checkAndMaintainConnections()
            connectionCheckHandler.postDelayed(this, connectionCheckInterval)
        }
    }

    private fun checkAndMaintainConnections() {
        // Get all available devices from trilateratingMacAddresses that are in range
        val availableDevices = beaconProjects.mapNotNull { address ->
            caughtInScan(address.key)?.let { scanResult ->
                Pair(address, scanResult)
            }
        }.sortedByDescending { it.second.rssi } // Sort by RSSI (strongest first)

        // Handle devices that are no longer in range
        val devicesToRemove = connectedDevices.filter { address ->
            !availableDevices.any { it.first.key == address }
        }
        devicesToRemove.forEach { address ->
            Timber.d("Device no longer in range: $address")
            connectedDevices.remove(address)
        }


        // Connect to new devices if we have capacity
        val availableSlots = maxConnections - connectedDevices.size
        if (availableSlots > 0) {
            availableDevices
                .filter { it.first.key !in connectedDevices }
                .take(availableSlots)
                .forEach { (address, scanResult) ->
                    Timber.d("Attempting to connect to device: $address (RSSI: ${scanResult.rssi})")
                    scanResult.device.let { device ->
                        ConnectionManager.connect(device, appContext)
                        connectedDevices.add(address.key)
                    }
                }
        }
//        else if (availableDevices.isNotEmpty()) {
//            // If we're at max connections but have stronger signals available,
//            // disconnect the weakest connected device and connect to the stronger one
//            val weakestConnectedDevice = connectedDevices.minByOrNull { address ->
//                availableDevices.find { it.first == address }?.second?.rssi ?: Int.MIN_VALUE
//            }
//
//            val strongestAvailableDevice = availableDevices.first()
//
//            if (weakestConnectedDevice != null) {
//                val weakestRssi = availableDevices.find { it.first == weakestConnectedDevice }?.second?.rssi ?: Int.MIN_VALUE
//                if (strongestAvailableDevice.second.rssi > weakestRssi) {
//                    Timber.d("Switching connection from $weakestConnectedDevice to ${strongestAvailableDevice.first} due to better signal")
//                    connectedDevices.remove(weakestConnectedDevice)
//                    strongestAvailableDevice.second.device.let { device ->
//                        ConnectionManager.connect(device, appContext)
//                        connectedDevices.add(strongestAvailableDevice.first)
//                    }
//                }
//            }
//        }
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

        // Start connection maintenance
        //connectionCheckHandler.post(connectionCheckRunnable)
        
        startScanCycle()
    }

    @SuppressLint("MissingPermission")
    fun stopScanning() {
        if (!isScanning || !::bluetoothAdapter.isInitialized) return

        handler.removeCallbacks(scanRunnable)
        connectionCheckHandler.removeCallbacks(connectionCheckRunnable)
        bleScanner.stopScan(bleScanCallback)
        isScanning = false
        continuousScanning = false
        //connectedDevices.forEach { address ->
        //    val device = bluetoothAdapter.getRemoteDevice(address)
        //    ConnectionManager.teardownConnection(device)
        //}
        connectedDevices.clear()
        Timber.d("Stopped BLE scan and connection maintenance")
    }

    fun isScanning(): Boolean = isScanning

    fun getCurrentResults(): List<ScanResult> = scanResults.toList()

//    fun rssiToDistance(beacon:ScanResult): Double {
//        // need to calibrate beacons
//        val calibrationRSSI = beaconProjects[beacon.device.address]?.toDouble()
//        val txPower = beacon.txPower.toDouble()
//        if (calibrationRSSI != null) {
//            return 10.0.pow((calibrationRSSI - beacon.rssi)/(10*txPower))
//        }
//        return 0.0
//    }


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

            scanResults.forEach { result ->
                if (result.rssi > -55) {
                    handleNearbyDevice(result)
                }
            }

            // Check and maintain connections
            //checkAndMaintainConnections()

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

    private fun initializeVibrator() {
        vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager = appContext.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
            vibratorManager.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            appContext.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }
    }

    private fun handleNearbyDevice(result: ScanResult) {
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