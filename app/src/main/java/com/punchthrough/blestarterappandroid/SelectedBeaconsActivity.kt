package com.punchthrough.blestarterappandroid

import android.bluetooth.le.ScanResult
import android.os.Bundle
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.punchthrough.blestarterappandroid.databinding.ActivitySelectedBeaconsBinding
import timber.log.Timber // Ensure this import is present
import kotlin.math.pow
import kotlin.math.sqrt
import android.widget.Toast

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

        binding.proceedButton.setOnClickListener {
            val coordinates = selectedBeaconsAdapter.getCoordinates()
            // Use the coordinates for trilateration or other purposes
            coordinates.forEach { (address, pair) ->
                Timber.i("Device: $address, X: ${pair.first}, Y: ${pair.second}")
            }
            
            // Calculate the user's location using trilateration
            calculateUserLocation(coordinates)
        }
    }

    // New method to get RSSI for a specific beacon address
    private fun getRssiForBeacon(address: String): Int {
        return selectedBeaconsAdapter.getRssiForDevice(address) ?: -100 // Return a default value if not found
    }

    // New method to calculate user location
    private fun calculateUserLocation(coordinates: Map<String, Pair<Double, Double>>) {
        // Check if there are at least three coordinates
        if (coordinates.size < 3) {
            Toast.makeText(this, "Please select at least 3 beacons for trilateration.", Toast.LENGTH_SHORT).show()
            return
        }

        // Assuming you have a method to get RSSI values for each beacon
        val distances = coordinates.keys.associateWith { address ->
            val rssi = getRssiForBeacon(address) // Get RSSI for the beacon
            calculateDistanceFromRssi(rssi) // Calculate distance from RSSI
        }

        // Perform trilateration
        val userLocation = trilaterate(coordinates, distances)

        // Start PointGraphActivity and pass the user location
        val intent = Intent(this, PointGraphActivity::class.java).apply {
            putExtra("USER_LOCATION_X", userLocation.first)
            putExtra("USER_LOCATION_Y", userLocation.second)
        }
        startActivity(intent)
    }

    private fun calculateDistanceFromRssi(rssi: Int): Double {
        val A = -59 // RSSI value at 1 meter (adjust based on your environment)
        val n = 2.0 // Path-loss exponent (adjust based on your environment)
        return 10.0.pow((A - rssi) / (10 * n))
    }

    private fun trilaterate(
        coordinates: Map<String, Pair<Double, Double>>,
        distances: Map<String, Double>
    ): Pair<Double, Double> {
        // Extract coordinates and distances
        val (x1, y1) = coordinates.values.elementAt(0)
        val (x2, y2) = coordinates.values.elementAt(1)
        val (x3, y3) = coordinates.values.elementAt(2)

        val r1 = distances.values.elementAt(0)
        val r2 = distances.values.elementAt(1)
        val r3 = distances.values.elementAt(2)

        // Trilateration formula
        val A = 2 * (x2 - x1)
        val B = 2 * (y2 - y1)
        val C = r1.pow(2) - r2.pow(2) - x1.pow(2) - y1.pow(2) + x2.pow(2) + y2.pow(2)
        val D = 2 * (x3 - x2)
        val E = 2 * (y3 - y2)
        val F = r2.pow(2) - r3.pow(2) - x2.pow(2) - y2.pow(2) + x3.pow(2) + y3.pow(2)

        val x = (C * E - F * B) / (E * A - B * D)
        val y = (C * D - A * F) / (B * D - A * E)

        return Pair(x, y)
    }
}
