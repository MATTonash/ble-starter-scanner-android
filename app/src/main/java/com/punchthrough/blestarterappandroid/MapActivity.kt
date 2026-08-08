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

import android.annotation.SuppressLint
import android.bluetooth.le.ScanResult
import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Build
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.util.Log
import android.view.GestureDetector
import android.view.MotionEvent
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import com.matt.guidebeacons.beacons.Beacon
import com.matt.guidebeacons.beacons.BeaconData
import com.matt.guidebeacons.services.BuzzerVibration
import com.matt.guidebeacons.services.NEARBY_BUZZER_RSSI


class MapActivity : AppCompatActivity() {

    private var initialAngleSet = false
    private var initialAngle = 0.00f
    private val bluetoothWorker = BluetoothWorkerClass.getInstance()
    private val beaconProjects = BeaconData.getBeaconProjects()

    private lateinit var gestureDetector: GestureDetector

    private lateinit var userMapView: UserMapView
    private lateinit var positionCalculator: PositionCalculator

    private lateinit var buzzer: BuzzerVibration
    private lateinit var vibrator: Vibrator

    private lateinit var sensorManager: SensorManager
    private lateinit var magnetometer: Sensor
    private lateinit var accelerometer: Sensor

    private lateinit var accelerometerReading: FloatArray
    private lateinit var magnetometerReading: FloatArray
    private lateinit var rotationMatrix: FloatArray
    private lateinit var userAngles: FloatArray

    @SuppressLint("ClickableViewAccessibility")
    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_map)

        userMapView = findViewById(R.id.user_map_view)
        userMapView.loadConfigFromRawXml(R.raw.user_map_config)

        positionCalculator = PositionCalculatorFactory.getCalculator()

        buzzer = BuzzerVibration(this)
        vibrator = buzzer.getVibrator()

        bluetoothWorker.initialize(this)
        startRssiTracking()

        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        if (sensorManager.getDefaultSensor(Sensor.TYPE_ALL) != null) {
            magnetometer = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)!!
            accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)!!
        } else {
            Log.d("Sensor fail", "Magnetic field and/or accelorometer not found")
        }

        accelerometerReading = FloatArray(3)
        magnetometerReading = FloatArray(3)
        rotationMatrix = FloatArray(9)
        userAngles = FloatArray(3)

        gestureDetector = GestureDetector(this, object : GestureDetector.SimpleOnGestureListener() {

            override fun onDown(e: MotionEvent): Boolean {
                return true // REQUIRED for long press to work
            }

            override fun onLongPress(e: MotionEvent) {
                userMapView.clearUserPath()
            }
        })

        // Drawing path
        userMapView.setOnTouchListener { view, event ->
            gestureDetector.onTouchEvent(event)
            val mapPoint = userMapView.screenToMap(event.x, event.y)
            view.performClick()
            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    userMapView.beginUserPath()
                    userMapView.addUserPathPoint(mapPoint)
                }
                MotionEvent.ACTION_MOVE -> {
                    userMapView.addUserPathPoint(mapPoint)
                }
            }
            true
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun startRssiTracking() {
        bluetoothWorker.startScanning(
            callback = { results ->
                handleScanResults(results)
            },
            continuous = true,
            //TODO: this is too frequent and will trigger the OS 30s timeout
            period = 9500L,    // Scan every second
            interval = 500L    // Small interval between scans
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
            .sortedByDescending {
                beaconProjects[it.device.address]?.updateFilteredRSSI(it.rssi)
                beaconProjects[it.device.address]?.getFilteredRSSI()
            }
            //.take(3) // Limit to top 3 beacons for performance

        // Need at least 1 beacon for trilateration
        if (knownResults.isEmpty()) {
            return
        }

        // Build coordinates, distances and collect visible beacons
        val coords = Array(knownResults.size) { DoubleArray(3) }
        val distances = DoubleArray(knownResults.size)
        val visibleBeacons = mutableListOf<Beacon>()

        knownResults.forEachIndexed { index, res ->
            val beacon = beaconProjects[res.device.address] ?: return@forEachIndexed
            coords[index] = beacon.getCoordinates()
            distances[index] = beacon.calculateDistance(res.rssi, 4, this)
            visibleBeacons.add(beacon)
        }

        userMapView.clearBeacons()
        userMapView.addBeacons(visibleBeacons.toTypedArray())
        solveForUser(coords, distances, visibleBeacons)

        rawResults.forEach { result ->
            if (result.rssi > NEARBY_BUZZER_RSSI) {
                buzzer.buzzForNearbyDevice(result)
            }
        }
    }


    // Source - https://stackoverflow.com/a/4128736
    // Posted by Mark B
    // Retrieved 2026-07-17, License - CC BY-SA 2.5
    private val SensorListener: SensorEventListener = object : SensorEventListener {
        override fun onSensorChanged(e: SensorEvent) {
            when (e.sensor.type) {
                Sensor.TYPE_ACCELEROMETER -> {
                    System.arraycopy(e.values, 0, accelerometerReading, 0, e.values.size)
                }
                Sensor.TYPE_MAGNETIC_FIELD -> {
                    System.arraycopy(e.values, 0, magnetometerReading, 0, e.values.size)
                }
            }

            if (SensorManager.getRotationMatrix(rotationMatrix, null, accelerometerReading, magnetometerReading)) {
                SensorManager.getOrientation(rotationMatrix, userAngles)

                // Update UI with new angle
                if (!initialAngleSet) {
                    initialAngleSet = true
                    initialAngle = Math.toDegrees(userAngles[0].toDouble()).toFloat()
                }
                val angle = (Math.toDegrees(userAngles[0].toDouble()) + initialAngle.toDouble()).toFloat()
                userMapView.setUserAngle(angle)
            }
        }
        override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
    }

    private fun alertUser() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(VibrationEffect.createOneShot(200, VibrationEffect.DEFAULT_AMPLITUDE))
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(200)
        }

    }

    /**
     * Updates user position based on given distances and coordinates:
     * each element in distances denotes how far the user is from the corresponding element in coords
     */
    private fun solveForUser(coords : Array<DoubleArray>, distances : DoubleArray, beacons: List<Beacon>) {
        // Create solver with current beacons and set distances
        val initial: DoubleArray = userMapView.getUserPosition()
//        trilaterationFunction = TrilaterationFunction(initial, coords, distances)
//
//        val userCoordinates = trilaterationFunction.solve()

        val position = positionCalculator.calculatePosition(beacons)

        userMapView.setUserPosition(position?.x?.toFloat() ?: initial[0].toFloat(), position?.y?.toFloat() ?: initial[1].toFloat(), 0.00f)
    }

    override fun onStart() { super.onStart() }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onResume() {
        super.onResume()
        // startRssiTracking()
        sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)?.also { accelerometer ->
            sensorManager.registerListener(
                SensorListener,
                accelerometer,
                SensorManager.SENSOR_DELAY_UI
            )
        }
        sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)?.also { magneticField ->
            sensorManager.registerListener(
                SensorListener,
                magneticField,
                SensorManager.SENSOR_DELAY_UI
            )
        }
    }

    override fun onPause() {
        super.onPause()
        bluetoothWorker.stopScanning()
        sensorManager.unregisterListener(SensorListener)
    }

    override fun onStop() { super.onStop() }

    override fun onDestroy() {
        super.onDestroy()
        bluetoothWorker.stopScanning()
    }

    override fun onLowMemory() { super.onLowMemory() }

    override fun onSaveInstanceState(outState: Bundle) { super.onSaveInstanceState(outState) }

}
