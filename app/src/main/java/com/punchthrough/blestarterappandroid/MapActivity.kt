/*
 * Copyright 2025 Punch Through Design LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.punchthrough.blestarterappandroid

import android.bluetooth.le.ScanResult
import android.os.Build
import android.os.Bundle
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import com.github.mikephil.charting.data.Entry


class MapActivity : AppCompatActivity() {

    private val bluetoothWorker = BluetoothWorkerClass.getInstance()
    private val beaconProjects = bluetoothWorker.getBeaconProjects()

    private var userPoints = ArrayList<Entry>()
    private lateinit var userMapView: UserMapView
    private lateinit var trilaterationFunction : TrilaterationFunction

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_map)

        userMapView = findViewById(R.id.user_map_view)
        userMapView.loadConfigFromRawXml(R.raw.user_map_config)

        bluetoothWorker.initialize(this)

        startRssiTracking()

        userMapView.setUserPosition(1.5.toFloat(), 2.5.toFloat())
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun startRssiTracking() {
        bluetoothWorker.startScanning(
            callback = { results ->
                handleScanResults(results)
            },
            continuous = true,
            period = 1000L,    // Scan every second
            interval = 200L    // Small interval between scans
        )
    }

    @RequiresApi(Build.VERSION_CODES.O)
    /**
     * Maps scanned devices to known mac addresses which each have a unique coordinate,
     * then converts rssi to get the distance from each beacon.
     * Then calls solveForUser to update user position
     * rawResults: list of scanned devices
     */
    private fun handleScanResults(rawResults: List<ScanResult>) {
        // Keep only known project beacons and sort by RSSI
        val knownResults = rawResults
            .filter { beaconProjects.containsKey(it.device.address) }
            .sortedByDescending { it.rssi }
            .take(3) // Limit to top 3 beacons for performance

        // Need at least 3 beacons for trilateration
        if (knownResults.size < 3) {
            return
        }

        // Build coordinates and distances arrays aligned by index
        val coords = Array(knownResults.size) { DoubleArray(0) }
        val distances = DoubleArray(knownResults.size)
        knownResults.forEachIndexed { index, res ->
            val beacon = beaconProjects[res.device.address] ?: return@forEachIndexed
            coords[index] = beacon.getCoordinates()
            distances[index] = beacon.calculateDistance(res.rssi, res.txPower)
        }

        solveForUser(coords, distances)
    }

    /**
     * Updates user position based on given distances and coordinates:
     * distances[i] denotes how far the user is from coords[i]
     */
    private fun solveForUser(coords : Array<DoubleArray>, distances : DoubleArray) {
        // Create solver with current beacons and set distances
        trilaterationFunction = TrilaterationFunction(coords)
        trilaterationFunction.setBeaconDistances(distances)

        val userCoordinates = trilaterationFunction.solve()

        userPoints.clear()
        userPoints.add(Entry(userCoordinates[0].toFloat(), userCoordinates[1].toFloat()))

        userMapView.setUserPosition(userCoordinates[0].toFloat(), userCoordinates[1].toFloat())
    }

    override fun onStart() { super.onStart() }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onResume() {
        super.onResume()
        // startRssiTracking()
    }

    override fun onPause() {
        super.onPause()
        bluetoothWorker.stopScanning()
    }

    override fun onStop() { super.onStop() }

    override fun onDestroy() {
        super.onDestroy()
        bluetoothWorker.stopScanning()
    }

    override fun onLowMemory() { super.onLowMemory() }

    override fun onSaveInstanceState(outState: Bundle) { super.onSaveInstanceState(outState) }

}
