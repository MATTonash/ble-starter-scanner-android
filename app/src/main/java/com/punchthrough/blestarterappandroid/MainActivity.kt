/*
 * Copyright 2024 Punch Through Design LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.punchthrough.blestarterappandroid

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.app.AlertDialog
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.UiThread
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat // Import for checking permissions
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.SimpleItemAnimator
import com.punchthrough.blestarterappandroid.ble.ConnectionEventListener
import com.punchthrough.blestarterappandroid.ble.ConnectionManager
import com.punchthrough.blestarterappandroid.databinding.ActivityMainBinding
import timber.log.Timber
import android.bluetooth.le.ScanFilter
import android.os.ParcelUuid
import android.os.VibratorManager // Import the VibratorManager class
import android.os.Build // Import Build for checking API levels
import android.os.Vibrator // Import the traditional Vibrator class

private const val PERMISSION_REQUEST_CODE = 1

class MainActivity : AppCompatActivity() {

    /*******************************************
     * Properties
     *******************************************/

    private lateinit var binding: ActivityMainBinding

    private val bluetoothAdapter: BluetoothAdapter by lazy {
        val bluetoothManager = getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        bluetoothManager.adapter
    }

    private val bleScanner by lazy {
        bluetoothAdapter.bluetoothLeScanner
    }

    private val scanSettings = ScanSettings.Builder()
        .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
        .build()

    private var isScanning = false
        set(value) {
            field = value
            runOnUiThread { binding.scanButton.text = if (value) "Stop Scan" else "Start Scan" }
        }

    private val scanResults = mutableListOf<ScanResult>()
    private val scanResultAdapter: ScanResultAdapter by lazy {
        ScanResultAdapter(scanResults) { result ->
            if (isScanning) {
                stopBleScan()
            }
            with(result.device) {
                Timber.w("Connecting to $address")
                ConnectionManager.connect(this, this@MainActivity)
            }
        }
    }

    private val bluetoothEnablingResult = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            Timber.i("Bluetooth is enabled, good to go")
        } else {
            Timber.e("User dismissed or denied Bluetooth prompt")
            promptEnableBluetooth()
        }
    }

    private lateinit var vibrator: Vibrator // Declare a Vibrator instance

    /*******************************************
     * Activity function overrides
     *******************************************/

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        }
        binding.scanButton.setOnClickListener { if (isScanning) stopBleScan() else startBleScan() }
        setupRecyclerView()

        // Add this new code
        // Remove or comment out the button click listener for trilateration
        // binding.trilaterationButton.setOnClickListener {
        //     val intent = Intent(this, trilateration::class.java)
        //     startActivity(intent)
        // }

        // Disable the proceed button to prevent navigation to SelectedBeaconsActivity
        binding.proceedButton.isEnabled = false // Disable the button
        binding.proceedButton.alpha = 0.5f // Optionally, change the button's appearance to indicate it's disabled

        binding.pointGraphButton.setOnClickListener {
            val intent = Intent(this, PointGraphActivity::class.java)
            startActivity(intent)
        }

        // Add this new button
        binding.proceedButton.setOnClickListener {
            // Remove selection logic since no beacons can be selected
            Toast.makeText(this, "No beacons can be selected", Toast.LENGTH_SHORT).show()
        }

        // Initialize the Vibrator based on API level
        vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            // For API level 31 and above, use VibratorManager
            val vibratorManager = getSystemService(VibratorManager::class.java)
            vibratorManager.defaultVibrator
        } else {
            // For lower API levels, use the traditional Vibrator
            getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }
    }

    override fun onResume() {
        super.onResume()
//        ConnectionManager.registerListener(connectionEventListener)
        if (!bluetoothAdapter.isEnabled) {
            promptEnableBluetooth()
        }
    }

    override fun onPause() {
        super.onPause()
        if (isScanning) {
            stopBleScan()
        }
//        ConnectionManager.unregisterListener(connectionEventListener)
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode != PERMISSION_REQUEST_CODE) {
            return
        }
        if (permissions.isEmpty() && grantResults.isEmpty()) {
            Timber.e("Empty permissions and grantResults array in onRequestPermissionsResult")
            Timber.w("This is likely a cancellation due to user interaction interrupted")
            return
        }

        // Log permission request outcomes
        val resultsDescriptions = grantResults.map {
            when (it) {
                PackageManager.PERMISSION_DENIED -> "Denied"
                PackageManager.PERMISSION_GRANTED -> "Granted"
                else -> "Unknown"
            }
        }
        Timber.w("Permissions: ${permissions.toList()}, grant results: $resultsDescriptions")

        // A denied permission is permanently denied if shouldShowRequestPermissionRationale is false
        val containsPermanentDenial = permissions.zip(grantResults.toTypedArray()).any {
            it.second == PackageManager.PERMISSION_DENIED &&
                !ActivityCompat.shouldShowRequestPermissionRationale(this, it.first)
        }
        val containsDenial = grantResults.any { it == PackageManager.PERMISSION_DENIED }
        val allGranted = grantResults.all { it == PackageManager.PERMISSION_GRANTED }
        when {
            containsPermanentDenial -> {
                Timber.e("User permanently denied granting of permissions")
                Timber.e("Requesting for manual granting of permissions from App Settings")
                promptManualPermissionGranting()
            }
            containsDenial -> {
                // It's still possible to re-request permissions
                requestRelevantBluetoothPermissions(PERMISSION_REQUEST_CODE)
            }
            allGranted && hasRequiredBluetoothPermissions() -> {
                startBleScan()
            }
            else -> {
                Timber.e("Unexpected scenario encountered when handling permissions")
                recreate()
            }
        }
    }

    /*******************************************
     * Private functions
     *******************************************/

    /**
     * Prompts the user to enable Bluetooth via a system dialog.
     *
     * For Android 12+, [Manifest.permission.BLUETOOTH_CONNECT] is required to use
     * the [BluetoothAdapter.ACTION_REQUEST_ENABLE] intent.
     */
    private fun promptEnableBluetooth() {
        if (hasRequiredBluetoothPermissions() && !bluetoothAdapter.isEnabled) {
            Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE).apply {
                bluetoothEnablingResult.launch(this)
            }
        }
    }

    @SuppressLint("MissingPermission, NotifyDataSetChanged") // Check performed inside extension fun
    private fun startBleScan() {
        if (!hasRequiredBluetoothPermissions()) {
            requestRelevantBluetoothPermissions(PERMISSION_REQUEST_CODE)
        } else {
            scanResults.clear()
            scanResultAdapter.updateList(scanResults)
            scanResultAdapter.notifyDataSetChanged()
            bleScanner.startScan(null, scanSettings, scanCallback)
            isScanning = true
        }
    }

    @SuppressLint("MissingPermission") // Check performed inside extension fun
    private fun stopBleScan() {
        if (hasRequiredBluetoothPermissions()) {
            bleScanner.stopScan(scanCallback)
            isScanning = false
        }
    }

    @UiThread
    private fun setupRecyclerView() {
        binding.scanResultsRecyclerView.apply {
            adapter = scanResultAdapter
            layoutManager = LinearLayoutManager(
                this@MainActivity,
                RecyclerView.VERTICAL,
                false
            )
            isNestedScrollingEnabled = false
            itemAnimator.let {
                if (it is SimpleItemAnimator) {
                    it.supportsChangeAnimations = false
                }
            }
        }
    }

    @UiThread
    private fun promptManualPermissionGranting() {
        AlertDialog.Builder(this)
            .setTitle(R.string.please_grant_relevant_permissions)
            .setMessage(R.string.app_settings_rationale)
            .setPositiveButton(R.string.app_settings) { _, _ ->
                try {
                    startActivity(
                        Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                            data = Uri.parse("package:$packageName")
                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        }
                    )
                } catch (e: ActivityNotFoundException) {
                    if (!isFinishing) {
                        Toast.makeText(
                            this,
                            R.string.cannot_launch_app_settings,
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
                finish()
            }
            .setNegativeButton(R.string.quit) { _, _ -> finishAndRemoveTask() }
            .setCancelable(false)
            .show()
    }

    /*******************************************
     * Callback bodies
     *******************************************/
    // Beacon names
    // 1. "80:EC:CC:CD:33:28" - Beacon EW1 - Project 1
    // 2. "80:EC:CC:CD:33:7C" - Beacon EW2 - Project 2
    // 3. "80:EC:CC:CD:33:7E" - Beacon EW3 - Project 3
    // 4. "80:EC:CC:CD:33:58" - Beacon EW6 - Project 4
    // 5. "EC:81:F6:64:F0:86" - Beacon Apple 06 - Project 5
    // 6. "EC:BF:E3:25:D5:6C" - Beacon Apple 02 - Project 6
    // 7. "E0:35:2F:E6:42:46" - Beacon Apple 04 - Project 7
    // 8. "CB:31:FE:48:1B:CB" - Beacon Apple 05 - Project 8
    // 9. "D8:F2:C8:9B:33:34" - RDL 04 - Project 9
    // 10. "00:3C:84:28:87:01" - RFstar-01 - Project 10
    // 11. "00:3C:84:28:77:AB" - RFstar-05 - Project 11
    val targetMacAddresses = listOf("80:EC:CC:CD:33:28",
                                    "80:EC:CC:CD:33:7C",
                                    "80:EC:CC:CD:33:7E",
                                    "80:EC:CC:CD:33:58",
                                    "EC:81:F6:64:F0:86",
                                    "EC:BF:E3:25:D5:6C",
                                    "E0:35:2F:E6:42:46",
                                    "CB:31:FE:48:1B:CB",
                                    "D8:F2:C8:9B:33:34",
                                    "00:3C:84:28:87:01",
                                    "00:3C:84:28:77:AB") // Replace with your target MAC addresses

    private val beaconProjects = mapOf(
        "80:EC:CC:CD:33:28" to "Project 1",
        "80:EC:CC:CD:33:7C" to "Project 2",
        "80:EC:CC:CD:33:7E" to "Project 3",
        "80:EC:CC:CD:33:58" to "Project 4",
        "EC:81:F6:64:F0:86" to "Project 5",
        "EC:BF:E3:25:D5:6C" to "Project 6",
        "E0:35:2F:E6:42:46" to "Project 7",
        "CB:31:FE:48:1B:CB" to "Project 8",
        "D8:F2:C8:9B:33:34" to "Project 9",
        "00:3C:84:28:87:01" to "Project 10",
        "00:3C:84:28:77:AB" to "Project 11"
    )


    // If we're getting a scan result, we already have the relevant permission(s)
    @SuppressLint("MissingPermission")
    private val scanCallback = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            // Check if the scanned device is already in the list
            if (result.device.address in targetMacAddresses) {
                val indexQuery = scanResults.indexOfFirst { it.device.address == result.device.address }
                if (indexQuery != -1) { // A scan result already exists with the same address
                    scanResults[indexQuery] = result // Update the existing result
                    scanResultAdapter.notifyItemChanged(indexQuery)
                } else {
                    // New device found, add it to the list
                    with(result.device) {
                        Timber.i("Found BLE device! Name: ${name ?: "Unnamed"}, address: $address")
                    }
                    scanResults.add(result) // Add the new result
                    scanResultAdapter.notifyItemInserted(scanResults.size - 1)
                }
                // Check RSSI value and show toast if below -40 dBm
                if (result.rssi > -40) {
                    Toast.makeText(this@MainActivity, "Close to ${ beaconProjects[result.device.address] ?: "Unknown Beacon"}", Toast.LENGTH_SHORT).show()
                    // Check if the VIBRATE permission is granted
                    if (ContextCompat.checkSelfPermission(this@MainActivity, Manifest.permission.VIBRATE) == PackageManager.PERMISSION_GRANTED) {
                        vibrator.vibrate(500) // Vibrate for 500 milliseconds
                    } else {
                        // Request the VIBRATE permission
                        ActivityCompat.requestPermissions(this@MainActivity, arrayOf(Manifest.permission.VIBRATE), PERMISSION_REQUEST_CODE)
                    }
                }
                // Sort the list by RSSI in descending order
                scanResults.sortByDescending { it.rssi }
                scanResultAdapter.notifyDataSetChanged() // Notify adapter of data change
            }
        }

        override fun onScanFailed(errorCode: Int) {
            Timber.e("onScanFailed: code $errorCode")
        }
    }

//    private val connectionEventListener by lazy {
//        ConnectionEventListener().apply {
//            onConnectionSetupComplete = { gatt ->
//                Intent(this@MainActivity, BleOperationsActivity::class.java).also {
//                    it.putExtra(BluetoothDevice.EXTRA_DEVICE, gatt.device)
//                    startActivity(it)
//                }
//            }
//            @SuppressLint("MissingPermission")
//            onDisconnect = {
//                val deviceName = if (hasRequiredBluetoothPermissions()) {
//                    it.name
//                } else {
//                    "device"
//                }
//                runOnUiThread {
//                    AlertDialog.Builder(this@MainActivity)
//                        .setTitle(R.string.disconnected)
//                        .setMessage(
//                            getString(R.string.disconnected_or_unable_to_connect_to_device, deviceName)
//                        )
//                        .setPositiveButton(R.string.ok, null)
//                        .show()
//                }
//            }
//        }
//    }
}
