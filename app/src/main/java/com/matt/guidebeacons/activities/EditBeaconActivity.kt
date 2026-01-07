package com.matt.guidebeacons.activities

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.snackbar.Snackbar
import com.matt.guidebeacons.beacons.Beacon
import com.matt.guidebeacons.beacons.BeaconData
import com.matt.guidebeacons.constants.*
import com.punchthrough.blestarterappandroid.databinding.RowSelectedBeaconBinding

class EditBeaconActivity : AppCompatActivity() {

    private lateinit var binding: RowSelectedBeaconBinding

    private var macAddress: String? = null
    private lateinit var beacon: Beacon

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = RowSelectedBeaconBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // TODO: handle null/missing/invalid MAC address (reuse for adding? but MAC address should still be populated)
        macAddress = intent.getStringExtra(INTENT_EXTRA_SELECTED_BEACON_MAC)
        beacon = BeaconData.getBeaconProjects()[macAddress] ?: Beacon("New beacon", 0, 0.0, 0.0)

        if (intent.getBooleanExtra(INTENT_EXTRA_ADDED_EXISTING_MAC, false)) {
            Snackbar.make(binding.root, "Editing existing beacon", Snackbar.LENGTH_SHORT).show()
        }

        populate()
    }

    private fun populate() {
        val macAddressTextView = binding.macAddress

        val deviceNameEditText = binding.deviceName
        val signalStrengthEditText = binding.signalStrength
        val xCoordinateEditText = binding.xCoordinate
        val yCoordinateEditText = binding.yCoordinate

        val saveButton = binding.saveEdits
        val deleteButton = binding.deleteBeacon

        macAddressTextView.text = macAddress
        deviceNameEditText.setText(beacon.toString())
        signalStrengthEditText.setText(beacon.getCalibrationRSSI().toString())
        xCoordinateEditText.setText(beacon.getCoordinates()[0].toString())
        yCoordinateEditText.setText(beacon.getCoordinates()[1].toString())

        saveButton.setOnClickListener {
            val newName: String = if (deviceNameEditText.text.isNullOrBlank()) beacon.toString() else deviceNameEditText.text.toString()
            val newRSSI: Int = signalStrengthEditText.text.toString().toIntOrNull() ?: beacon.getCalibrationRSSI()
            val newX: Double = xCoordinateEditText.text.toString().toDoubleOrNull() ?: beacon.getCoordinates()[0]
            val newY: Double = yCoordinateEditText.text.toString().toDoubleOrNull() ?: beacon.getCoordinates()[1]

            beacon.updateData(newName, newRSSI, newX, newY)
            setResult(RESULT_OK)
            finish()
        }

        deleteButton.setOnClickListener {
            BeaconData.getBeaconProjects().remove(macAddress)
            setResult(RESULT_OK)
            finish()
        }
    }
}