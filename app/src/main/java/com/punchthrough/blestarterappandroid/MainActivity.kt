package com.punchthrough.blestarterappandroid

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.bluetooth.le.ScanResult
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.Vibrator
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.UiThread
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.SimpleItemAnimator
import com.matt.guidebeacons.activities.CalibrationActivity
import com.matt.guidebeacons.beacons.BeaconData
import com.punchthrough.blestarterappandroid.databinding.ActivityMainBinding
import timber.log.Timber

private const val PERMISSION_REQUEST_CODE = 1

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private val bluetoothWorker = BluetoothWorkerClass.getInstance()

    private val beaconProjects = BeaconData.getBeaconProjects()
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
                //ConnectionManager.connect(this, this@MainActivity)
            }
        }
    }

    private var topThreeDevices = mutableListOf<String>()

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
        isScanning = false

        Timber.plant(Timber.DebugTree()) // show Timber log messages in Logcat

        BeaconData.initialiseBeaconData(this, "beacons.json")

        // Initialize BluetoothWorker
        bluetoothWorker.initialize(this)

        // Setup UI
        setupRecyclerView()

        setupScanButton()
        //initializeVibrator()

        // only setup viewmap button when 3 beacons collected

        setupViewMapButton()

        setupCalibrationButton()
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

    private fun allowClickViewMapButton() : Boolean {
        return scanResults.size >= 3
    }
    private fun setupViewMapButton() {
        binding.viewMapButton.setEnabled(allowClickViewMapButton())
        binding.viewMapButton.setOnClickListener {
            launchPointGraphActivity()
        }
    }

    private fun setupCalibrationButton() {
        binding.calibrationButton.setOnClickListener {
            launchCalibrationActivity()
        }
    }

    //private fun initializeVibrator() {
    //    vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
    //        val vibratorManager = getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
    //        vibratorManager.defaultVibrator
    //    } else {
    //        @Suppress("DEPRECATION")
     //       getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
    //    }
    //}

    override fun onResume() {
        super.onResume()
        if (!hasRequiredBluetoothPermissions()) {
            requestRelevantBluetoothPermissions(PERMISSION_REQUEST_CODE)
        }
    }

    override fun onPause() {
        super.onPause()
        isScanning = false
        // stopBleScan()
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
    }

    @SuppressLint("LogNotTimber")
    private fun handleScanResults(results: List<ScanResult>) {
        runOnUiThread {
            scanResults.clear()
            scanResults.addAll(results)

            // Process each result for notifications
            //results.forEach { result ->
            //    if (result.rssi > -55) {
            //        handleNearbyDevice(result)
            //    }
            //}

            if (allowClickViewMapButton()){
                setupViewMapButton()
            } else {
                binding.viewMapButton.setEnabled(false)
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
                "Close to ${beaconProjects[result.device.address] ?: "Unknown Beacon"}",
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

    private fun launchPointGraphActivity() {
        val pointGraphIntent = Intent(this, PointGraphActivity::class.java)
        startActivity(pointGraphIntent)
    }

    private fun launchCalibrationActivity() {
        val calibrationIntent = Intent(this, CalibrationActivity::class.java)
        startActivity(calibrationIntent)
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