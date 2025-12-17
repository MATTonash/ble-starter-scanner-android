package com.punchthrough.blestarterappandroid

import android.bluetooth.le.ScanResult
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString
import java.io.File

class RssiMappingActivity : AppCompatActivity() {

    private lateinit var beaconSpinner: Spinner
    private lateinit var rssiTextView: TextView
    private lateinit var distanceEditText: EditText
    private lateinit var saveButton: Button
    private lateinit var debugTextView: TextView

    private val bluetoothWorker = BluetoothWorkerClass.getInstance()
    private val beaconProjects = bluetoothWorker.getBeaconProjects()
    private var selectedBeacon: String? = null
    private var currentRssi: Int? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_rssi_mapping)

        // Initialize UI components
        beaconSpinner = findViewById(R.id.beacon_spinner)
        rssiTextView = findViewById(R.id.rssi_text_view)
        distanceEditText = findViewById(R.id.distance_edit_text)
        saveButton = findViewById(R.id.save_button)
        debugTextView = findViewById(R.id.debug_text_view)

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
            }

            override fun onNothingSelected(parent: AdapterView<*>) {
                selectedBeacon = null
            }
        }
    }

    private fun setupSaveButton() {
        saveButton.setOnClickListener {
            val distance = distanceEditText.text.toString().toDoubleOrNull()
            if (selectedBeacon != null && currentRssi != null && distance != null) {
                val debugInfo = "Beacon: $selectedBeacon, RSSI: $currentRssi, Distance: $distance"
                val json = Json.encodeToString(BeaconData(selectedBeacon!!, currentRssi!!, distance))
                debugTextView.text = json
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

    private fun handleScanResults(results: List<ScanResult>) {
        val selectedResult = results.find { it.device.address == selectedBeacon }
        if (selectedResult != null) {
            currentRssi = selectedResult.rssi
            runOnUiThread {
                rssiTextView.text = "RSSI: ${currentRssi ?: "N/A"}"
            }
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

    @Serializable
    data class BeaconData(var beaconAddress: String, var rssi: Int, var distance: Double)
}