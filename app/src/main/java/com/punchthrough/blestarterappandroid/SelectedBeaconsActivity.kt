package com.punchthrough.blestarterappandroid

import android.bluetooth.le.ScanResult
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.punchthrough.blestarterappandroid.databinding.ActivitySelectedBeaconsBinding
import timber.log.Timber // Ensure this import is present

class SelectedBeaconsActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySelectedBeaconsBinding
    private lateinit var selectedBeaconsAdapter: SelectedBeaconsAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySelectedBeaconsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val selectedBeacons = intent.getParcelableArrayListExtra<ScanResult>("SELECTED_BEACONS")

        selectedBeaconsAdapter = SelectedBeaconsAdapter(selectedBeacons ?: emptyList())
        binding.selectedBeaconsRecyclerView.apply {
            adapter = selectedBeaconsAdapter
            layoutManager = LinearLayoutManager(this@SelectedBeaconsActivity)
        }

        // Ensure the button ID matches the layout
        binding.proceedButton.setOnClickListener {
            val coordinates = selectedBeaconsAdapter.getCoordinates()
            // Use the coordinates for trilateration or other purposes
            coordinates.forEach { (address, pair) ->
                Timber.i("Device: $address, X: ${pair.first}, Y: ${pair.second}")
            }
        }
    }
}