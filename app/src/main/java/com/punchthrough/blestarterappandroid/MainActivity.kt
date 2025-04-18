package com.punchthrough.blestarterappandroid

import android.Manifest
import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.bluetooth.le.ScanResult
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.Vibrator
import android.os.VibratorManager
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.SimpleItemAnimator
import com.punchthrough.blestarterappandroid.databinding.ActivityMainBinding
import timber.log.Timber

private const val PERMISSION_REQUEST_CODE = 1

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private val bluetoothWorker = BluetoothWorkerClass.getInstance()
    private var isScanning = false
        set(value) {
            field = value
            runOnUiThread { binding.scanButton.text = if (value) "Stop Scan" else "Start Scan" }
        }

    private val scanResults = mutableListOf<ScanResult>()
    private val scanResultAdapter: ScanResultAdapter by lazy {
        ScanResultAdapter(scanResults) { result ->
            // Launch PointGraphActivity when a device is clicked
            launchPointGraphActivity(result)
        }
    }

    private lateinit var vibrator: Vibrator
    private var isToastShowing = false

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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Initialize BluetoothWorker
        bluetoothWorker.initialize(this)

        // Setup UI
        setupRecyclerView()
        setupScanButton()
        initializeVibrator()

//        binding.viewMapButton.setOnClickListener {
//            val intent = Intent(this, mapView::class.java)
//            startActivity(intent)
//        }
    }

    private fun setupScanButton() {
        binding.scanButton.setOnClickListener {
            if (isScanning) {
                stopBleScan()
            } else {
                startBleScan()
            }
        }
    }

    private fun initializeVibrator() {
        vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager = getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
            vibratorManager.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }
    }

    override fun onResume() {
        super.onResume()
        if (!hasRequiredBluetoothPermissions()) {
            requestRelevantBluetoothPermissions(PERMISSION_REQUEST_CODE)
        }
    }

    override fun onPause() {
        super.onPause()
        stopBleScan()
    }

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

    private fun startBleScan() {
        if (!hasRequiredBluetoothPermissions()) {
            requestRelevantBluetoothPermissions(PERMISSION_REQUEST_CODE)
            return
        }

        scanResults.clear()
        scanResultAdapter.updateList(scanResults)

        bluetoothWorker.startScanning(
            callback = { results ->
                handleScanResults(results)
            },
            continuous = true,
            period = 5000L,    // Scan for 5 seconds
            interval = 2000L   // Wait 2 seconds between scans
        )
        isScanning = true
    }

    private fun stopBleScan() {
        bluetoothWorker.stopScanning()
        isScanning = false
        // binding.viewMapButton.isClickable = false
    }

    private fun handleScanResults(results: List<ScanResult>) {
        runOnUiThread {
            scanResults.clear()
            scanResults.addAll(results)

            // Process each result for notifications
            results.forEach { result ->
                if (result.rssi > -50) {
                    handleNearbyDevice(result)
                }
            }

            // Sort and update the display
            scanResults.sortByDescending { it.rssi }
            scanResultAdapter.updateList(scanResults)
        }
    }

    private fun handleNearbyDevice(result: ScanResult) {
        if (!isToastShowing) {
            Toast.makeText(
                this,
                "Close to ${result.device.address ?: "Unknown Beacon"}",
                Toast.LENGTH_SHORT
            ).show()
            isToastShowing = true

            Handler(Looper.getMainLooper()).postDelayed({
                isToastShowing = false
            }, Toast.LENGTH_SHORT.toLong())

            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.VIBRATE
                ) == PackageManager.PERMISSION_GRANTED
            ) {
                vibrator.vibrate(500)
            }
        }
    }

    private fun launchPointGraphActivity(scanResult: ScanResult) {
        val intent = Intent(this, PointGraphActivity::class.java).apply {
            putExtra("TARGET_DEVICE_ADDRESS", scanResult.device.address)
            putExtra("DEVICE_NAME", scanResult.device.address ?: "Unknown Beacon")
            putExtra("INITIAL_RSSI", scanResult.rssi)
        }
        startActivity(intent)
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

        val resultsDescriptions = grantResults.map {
            when (it) {
                PackageManager.PERMISSION_DENIED -> "Denied"
                PackageManager.PERMISSION_GRANTED -> "Granted"
                else -> "Unknown"
            }
        }
        Timber.w("Permissions: ${permissions.toList()}, grant results: $resultsDescriptions")

        val containsPermanentDenial = permissions.zip(grantResults.toTypedArray()).any {
            it.second == PackageManager.PERMISSION_DENIED &&
                !ActivityCompat.shouldShowRequestPermissionRationale(this, it.first)
        }
        val containsDenial = grantResults.any { it == PackageManager.PERMISSION_DENIED }
        val allGranted = grantResults.all { it == PackageManager.PERMISSION_GRANTED }

        when {
//            containsPermanentDenial -> {
//                promptManualPermissionGranting()
//            }
            containsDenial -> {
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

    private fun promptEnableBluetooth() {
        if (hasRequiredBluetoothPermissions()) {
            Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE).apply {
                bluetoothEnablingResult.launch(this)
            }
        }
    }

//    private fun promptManualPermissionGranting() {
//        AlertDialog.Builder(this)
//            .setTitle(R.string.bluetooth_permission_required)
//            //.setMessage(R.string.bluetooth_permission_denied_permanently)
//            .setPositiveButton(R.string.open_settings) { _, _ ->
//                try {
//                    Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
//                        data = Uri.fromParts("package", packageName, null)
//                        startActivity(this)
//                    }
//                } catch (e: ActivityNotFoundException) {
//                    Timber.e("Could not open Settings: $e")
//                }
//            }
//            .setNegativeButton(R.string.quit) { _, _ -> finishAndRemoveTask() }
//            .setCancelable(false)
//            .show()
//    }
}