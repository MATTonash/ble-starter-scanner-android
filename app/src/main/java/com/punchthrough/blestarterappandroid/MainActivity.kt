package com.punchthrough.blestarterappandroid

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.le.ScanResult
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.UiThread
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.SimpleItemAnimator
import com.matt.guidebeacons.activities.AdminPanelActivity
import com.matt.guidebeacons.activities.PermissionsCheckActivity
import com.matt.guidebeacons.beacons.BeaconData
import com.matt.guidebeacons.constants.FILE_NAME_BEACONS
import com.matt.guidebeacons.services.BuzzerVibration
import com.matt.guidebeacons.services.NEARBY_BUZZER_RSSI
import com.punchthrough.blestarterappandroid.databinding.ActivityMainBinding
import timber.log.Timber

private const val PERMISSION_REQUEST_CODE = 1
private const val MIN_BEACONS_FOR_LOCATION = 0

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private val bluetoothWorker = BluetoothWorkerClass.getInstance()

    private val beaconProjects = BeaconData.getBeaconProjects()

    private val scanResults = mutableListOf<ScanResult>()
    private val scanResultAdapter: ScanResultAdapter by lazy {
        ScanResultAdapter(scanResults) { result ->
            if (bluetoothWorker.isScanning()) {
                stopBleScan()
            }
            with(result.device) {
                Timber.w("Connecting to $address")
                //ConnectionManager.connect(this, this@MainActivity)
            }
        }
    }

    private var topThreeDevices = mutableListOf<String>()

    private lateinit var buzzer: BuzzerVibration

    private val bluetoothEnablingResult = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
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

        if (Timber.treeCount() <= 0) Timber.plant(Timber.DebugTree()) // show Timber log messages in Logcat

        BeaconData.initialiseBeaconData(this, FILE_NAME_BEACONS)

        // Initialize BluetoothWorker
        bluetoothWorker.initialize(this)
        buzzer = BuzzerVibration(this)

        // Use the toolbar from the layout as the Activity's app bar so we control logo/title
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayShowHomeEnabled(true)
        // logo is provided via app:logo in the toolbar; set the toolbar title to the navigation page title
       supportActionBar?.title =""

        // Setup UI
        setupScanButton()
        setupRecyclerView()
        setupViewMapButton()
        setUpActivityButtons()
    }

    private fun setupScanButton() {
        binding.scanButton.setOnClickListener {
            if (bluetoothWorker.isScanning()) {
                stopBleScan()
            } else {
                startBleScan()
            }
            updateScanButton()
        }
    }

    private fun updateScanButton() {
        runOnUiThread {
            binding.scanButton.text = if (bluetoothWorker.isScanning()) "Stop Scan" else "Start Scan"
        }
    }

    private fun allowClickViewMapButton() : Boolean {
        return scanResults.size >= MIN_BEACONS_FOR_LOCATION
    }
    private fun setupViewMapButton() {
        // only setup viewmap button when 3 beacons collected
        binding.viewMapButton.isEnabled = allowClickViewMapButton()
        binding.viewMapButton.setOnClickListener {
            startActivity(Intent(this, MapActivity::class.java))
        }
    }

    private fun setUpActivityButtons() {
        setUpActivityButton(binding.adminPanelButton, AdminPanelActivity::class.java)
        setUpActivityButton(binding.permissionsDebugButton, PermissionsCheckActivity::class.java)
    }

    private fun setUpActivityButton(button: android.widget.Button, activity: Class<*>) {
        button.setOnClickListener {
            val intent = Intent(this, activity)
            startActivity(intent)
        }
    }

    override fun onPause() {
        super.onPause()
        stopBleScan()
    }

    override fun onResume() {
        super.onResume()
        updateScanButton()
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
        if (!hasRequiredRuntimePermissions()) {
            requestRequiredRuntimePermissions(PERMISSION_REQUEST_CODE)
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
    }

    private fun stopBleScan() {
        bluetoothWorker.stopScanning()
        updateScanButton()
    }

    @SuppressLint("LogNotTimber")
    private fun handleScanResults(results: List<ScanResult>) {
        runOnUiThread {
            scanResults.clear()
            scanResults.addAll(results)

            // Process each result for notifications
            results.forEach { result ->
                if (result.rssi > NEARBY_BUZZER_RSSI) {
                    buzzer.buzzForNearbyDevice(result)
                }
            }

            if (allowClickViewMapButton()){
                setupViewMapButton()
            } else {
                binding.viewMapButton.isEnabled = false
            }

            // Sort and update the display
            scanResults.sortByDescending { it.rssi }

            scanResultAdapter.updateList(scanResults)
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode != PERMISSION_REQUEST_CODE) return

        if (permissions.isEmpty() && grantResults.isEmpty()) {
            Timber.w("Empty permissions and grantResults array in onRequestPermissionsResult" +
                "\nThis is likely a cancellation due to user interaction interrupted")
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

        when {
            containsPermanentDenial -> {
                Timber.e("A required permission has been permanently denied and needs to be manually granted")
                promptManualPermissionGranting()
            }
            !hasRequiredRuntimePermissions() -> {
                requestRequiredRuntimePermissions(PERMISSION_REQUEST_CODE)
            }
            hasRequiredRuntimePermissions() -> {
                Timber.d("All required permissions granted")
            }
            else -> {
                Timber.e("Unexpected scenario encountered when handling permissions")
                recreate()
            }
        }
    }

    private fun promptEnableBluetooth() {
        if (hasRequiredRuntimePermissions()) {
            Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE).apply {
                bluetoothEnablingResult.launch(this)
            }
        }
    }

    private fun promptManualPermissionGranting() {
        AlertDialog.Builder(this)
            .setTitle(R.string.bluetooth_permission_required)
            .setMessage(R.string.bluetooth_permission_denied_permanently)
    }
}