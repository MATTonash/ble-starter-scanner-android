package com.punchthrough.blestarterappandroid

import android.bluetooth.le.ScanResult
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.addTextChangedListener
import com.matt.guidebeacons.beacons.RssiCollection
import com.matt.guidebeacons.beacons.RssiValue
import com.punchthrough.blestarterappandroid.databinding.ActivityRssiMappingBinding

class RssiMappingActivity : AppCompatActivity() {

    private lateinit var beaconSpinner: Spinner
    private lateinit var rssiTextView: TextView
    private lateinit var distanceEditText: EditText
    private lateinit var saveButton: Button
    private lateinit var recordButton: Button
    private lateinit var debugTextView: TextView

    private val bluetoothWorker = BluetoothWorkerClass.getInstance()
    private val beaconProjects = com.matt.guidebeacons.beacons.BeaconData.getBeaconProjects()

    private var selectedBeacon: String? = null
    private var rssiCollection: RssiCollection? = null
    private var currentRssi: Int? = null

    private var recording: Boolean = false
        set(value) {
            field = value
            recordButton.text = if (value) "Finish" else "Recording (average)"
            beaconSpinner.isEnabled = !value
            saveButton.isEnabled = !value
            distanceEditText.isEnabled = !value
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val binding = ActivityRssiMappingBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Initialize UI components
        beaconSpinner = binding.beaconSpinner
        rssiTextView = binding.rssiTextView
        distanceEditText = binding.distanceEditText
        saveButton = binding.saveButton
        recordButton = binding.recordButton
        debugTextView = binding.debugTextView

        setupBeaconSpinner()
        setupSaveButton()
        setupRecordButton()
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
                rssiCollection!!.getMeasurements().add(RssiValue(currentRssi!!.toDouble(), distance, RssiValue.CollectionType.SNAPSHOT))
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

    private fun updateRecordButtonEnabled() {
        recordButton.isEnabled = (distanceEditText.text.toString().toDoubleOrNull() != null)
    }

    private fun setupRecordButton() {
        updateRecordButtonEnabled()
        distanceEditText.addTextChangedListener { updateRecordButtonEnabled() }

        recordButton.setOnClickListener {
            recording = !recording

            if (!recording) {
                if (rssiCollection == null) {
                    timber.log.Timber.w("err: no rssi collection")
                    Toast.makeText(this, "err: no rssi collection", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }
                val distance = distanceEditText.text.toString().toDouble()
                val averageRssi = calculateRecordedRssiAverage(rssiCollection!!, distance, true)
                if (averageRssi != null) {
                    rssiCollection!!.getMeasurements().add(RssiValue(averageRssi, distance, RssiValue.CollectionType.AVERAGE))
                    rssiCollection!!.writeToFile(this, true)
                    debugTextView.text = "Beacon: $selectedBeacon, RSSI: $averageRssi, Distance: $distance"
                }
            }
        }
    }

    private fun startRssiTracking() {
        bluetoothWorker.startScanning(
            callback = { results ->
                handleScanResults(results)
            },
            continuous = true,
            period = 5000L,
            interval = 2000L
        )
    }

    private fun setCurrentRssi(rssi: Int?) {
        currentRssi = rssi
        runOnUiThread {
            rssiTextView.text = "RSSI: ${currentRssi ?: "N/A"}"
        }
    }

    private fun calculateRecordedRssiAverage(rssiCollection: RssiCollection, recordedDistance: Double, deleteRecordedValues: Boolean): Double? {
        val minimumCollectedValuesCount = 5
        val predicate : (RssiValue) -> Boolean = { it.getType() == RssiValue.CollectionType.RECORDING && it.getMeasuredDistance() == recordedDistance }

        val values = rssiCollection.getMeasurements()
        var count = 0
        var sum = 0.0
        for (value in values) {
            if (predicate(value)) {
                count++
                sum += value.getMeasuredRssi()
            }
        }

        if (count < minimumCollectedValuesCount) {
            timber.log.Timber.w("err: not enough rssi values")
            Toast.makeText(this, "err: not enough rssi values", Toast.LENGTH_SHORT).show()
            return null
        }

        if (deleteRecordedValues) {
            rssiCollection.getMeasurements().removeAll { predicate(it) }
        }

        return sum / count
    }

    private fun handleScanResults(results: List<ScanResult>) {
        val selectedResult = results.find { it.device.address == selectedBeacon }
        if (selectedResult != null) {
            setCurrentRssi(selectedResult.rssi)

            if (recording) {
                val distance = distanceEditText.text.toString().toDoubleOrNull()
                if (rssiCollection != null && currentRssi != null && distance != null) {
                    rssiCollection!!.getMeasurements().add(RssiValue(currentRssi!!.toDouble(), distance, RssiValue.CollectionType.RECORDING))
                }
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
}