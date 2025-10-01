package com.matt.guidebeacons.activities

import android.os.Bundle
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.matt.guidebeacons.beacons.Beacon
import com.matt.guidebeacons.beacons.BeaconData
import com.punchthrough.blestarterappandroid.R
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
        macAddress = intent.getStringExtra("SELECTED_BEACON_MAC")
        beacon = BeaconData.getBeaconProjects()[macAddress] ?: Beacon("New beacon", 0, 0.0, 0.0)

        populate()
    }

    private fun populate() {
        val deviceNameTextView: TextView = findViewById(R.id.device_name)
        val macAddressTextView: TextView = findViewById(R.id.mac_address)
        val signalStrengthTextView: TextView = findViewById(R.id.signal_strength)

        val xCoordinateEditText: EditText = findViewById(R.id.x_coordinate)
        val yCoordinateEditText: EditText = findViewById(R.id.y_coordinate)

        deviceNameTextView.text = beacon.toString()
        macAddressTextView.text = macAddress
        signalStrengthTextView.text = beacon.getCalibrationRSSI().toString()
        xCoordinateEditText.setText(beacon.getCoordinates()[0].toString())
        yCoordinateEditText.setText(beacon.getCoordinates()[1].toString())
    }
}