package com.punchthrough.blestarterappandroid

import android.bluetooth.le.ScanResult
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.matt.guidebeacons.beacons.RssiCollection
import com.matt.guidebeacons.beacons.RssiValue
import com.punchthrough.blestarterappandroid.databinding.ActivityRssiMappingBinding

class RssiMappingActivity : AppCompatActivity() {

    private lateinit var beaconSpinner: Spinner
    private lateinit var rssiTextView: TextView
    private lateinit var distanceEditText: EditText
    private lateinit var saveButton: Button
    private lateinit var debugTextView: TextView

    private val bluetoothWorker = BluetoothWorkerClass.getInstance()
    private val beaconProjects = com.matt.guidebeacons.beacons.BeaconData.getBeaconProjects()

    private var selectedBeacon: String? = null
    private var rssiCollection: RssiCollection? = null
    private var currentRssi: Int? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val binding = ActivityRssiMappingBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Initialize UI components
        beaconSpinner = binding.beaconSpinner
        rssiTextView = binding.rssiTextView
        distanceEditText = binding.distanceEditText
        saveButton = binding.saveButton
        debugTextView = binding.debugTextView

        setupBeaconSpinner()
        setupSaveButton()
        startRssiTracking()
    }

    private fun setupBeaconSpinner() {
        val beaconAddresses = beaconProjects.keys.toList()
        val beaconNames = beaconProjects.values.map{ it.toString()}
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, beaconNames)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        beaconSpinner.adapter = adapter

        beaconSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                selectedBeacon = beaconAddresses[position]
                rssiCollection = RssiCollection.readFromFile(
                    this@RssiMappingActivity,
                    selectedBeacon!!,
                    beaconProjects[selectedBeacon].toString()
                )
                // Clear currentRssi to prevent saving scan results from a different beacon
                setCurrentRssi(null)
            }

            override fun onNothingSelected(parent: AdapterView<*>) {
                selectedBeacon = null
                rssiCollection = null
                // Clear currentRssi to prevent saving scan results from a different beacon
                setCurrentRssi(null)
            }
        }
    }

    private fun setupSaveButton() {
        saveButton.setOnClickListener {
            val distance = distanceEditText.text.toString().toDoubleOrNull()
            if (selectedBeacon != null && currentRssi != null && distance != null) {
                val debugInfo = "Beacon: $selectedBeacon, RSSI: $currentRssi, Distance: $distance"
                rssiCollection!!.getMeasurements().add(RssiValue(currentRssi!!.toDouble(), distance))
                val result = rssiCollection!!.writeToFile(this, true)
                debugTextView.text = result
                // Clear currentRssi to prevent saving an old scan result
                setCurrentRssi(null)
                Toast.makeText(this, "Saved: $debugInfo", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "Please select a beacon, collect RSSI, and enter a distance", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun startRssiTracking() {
        bluetoothWorker.startScanning(
            callback = { results ->
                handleScanResults(results)
            },
            continuous = true,
            period = 1000L,
            interval = 200L
        )
    }

    private fun setCurrentRssi(rssi: Int?) {
        currentRssi = rssi
        runOnUiThread {
            rssiTextView.text = "RSSI: ${currentRssi ?: "N/A"}"
        }
    }

    private fun handleScanResults(results: List<ScanResult>) {
        val selectedResult = results.find { it.device.address == selectedBeacon }
        if (selectedResult != null) {
            setCurrentRssi(selectedResult.rssi)
        }
    }

    override fun onPause() {
        super.onPause()
        bluetoothWorker.stopScanning()
    }

    override fun onDestroy() {
        super.onDestroy()
        bluetoothWorker.stopScanning()
    }
}