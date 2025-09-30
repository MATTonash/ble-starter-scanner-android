package com.matt.guidebeacons.activities

import android.os.Bundle
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.punchthrough.blestarterappandroid.R
import com.punchthrough.blestarterappandroid.databinding.RowSelectedBeaconBinding

class EditBeaconActivity : AppCompatActivity() {

    private lateinit var binding: RowSelectedBeaconBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = RowSelectedBeaconBinding.inflate(layoutInflater)
        setContentView(binding.root)
    }

    private fun populate() {
        val deviceNameTextView: TextView = findViewById(R.id.device_name)
        val macAddressTextView: TextView = findViewById(R.id.mac_address)
        val signalStrengthTextView: TextView = findViewById(R.id.signal_strength)

        val xCoordinateEditText: EditText = findViewById(R.id.x_coordinate)
        val yCoordinateEditText: EditText = findViewById(R.id.y_coordinate)

        deviceNameTextView.text = "A"
    }
}