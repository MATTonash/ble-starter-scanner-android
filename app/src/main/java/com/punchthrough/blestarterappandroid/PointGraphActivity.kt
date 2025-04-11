package com.punchthrough.blestarterappandroid

import android.bluetooth.le.ScanResult
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet

class PointGraphActivity : AppCompatActivity() {

//    private lateinit var lineChart: LineChart
//    private lateinit var addButton: Button
//    private lateinit var xValueInput: EditText
//    private lateinit var yValueInput: EditText
    private val handler = Handler(Looper.getMainLooper())
    private lateinit var updateRunnable: Runnable
    private var currentEntry: Entry? = null // Store the current entry

    /*******************************************
     * Properties
     *******************************************/

    private val bluetoothWorker = BluetoothWorkerClass.getInstance()
    private lateinit var lineChart: LineChart

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_point_graph)

        // Initialize the worker if not already initialized
        bluetoothWorker.initialize(this)

        // Start or continue scanning
        bluetoothWorker.startScanning { results ->
            // Update your map view with the latest scan results
            results.forEach { scanResult ->
                // Update map markers or other UI elements based on scan results
                updateMapWithBeaconLocation(scanResult)
            }
        }
        setContentView(R.layout.activity_point_graph)

        lineChart = findViewById(R.id.lineChart)

        // Get user location from intent - this was just trying to get the last rssi from mainactivity, now we have workerclass
//        val userX = intent.getDoubleExtra("USER_LOCATION_X", 0.0)
//        val userY = intent.getDoubleExtra("USER_LOCATION_Y", 0.0)

        // Add initial user location to the graph
        // These are buttons and use old logic without worker class
//            currentEntry = Entry(userX.toFloat(), userY.toFloat())
//            updateChart()
//
//            addButton.setOnClickListener {
//                addPoint()
//            }

        // Start updating the user's position
//            startUpdatingPosition()
    }

    private fun updateMapWithBeaconLocation(scanResult: ScanResult) {
        // Your logic to update the map based on scan results
        // For example, updating markers based on RSSI values
        // TODO: map scanResult RSSI, set x = 0, y = rssi
        if (scanResult != null) {
            currentEntry = Entry(0.0F, scanResult.rssi.toFloat())
            val dataSet = LineDataSet(listOfNotNull(currentEntry), "Current Position") // Only show the current entry
            dataSet.setDrawCircles(true) // Show circles for points
            dataSet.setDrawValues(false) // Do not show values on the points
            lineChart.data = LineData(dataSet)
            lineChart.invalidate() // Refresh the chart
//            xValueInput.text.clear()
//            yValueInput.text.clear()
        } else {
            Toast.makeText(this, "Please enter valid values", Toast.LENGTH_SHORT).show()
        }

    }
}

//    private val scanResultAdapter: ScanResultAdapter by lazy {
//        ScanResultAdapter(scanResults) { result ->
//            if (isScanning) {
//                stopBleScan()
//            }
//            with(result.device) {
//                Timber.w("Connecting to $address")
//                ConnectionManager.connect(this, this@MainActivity)
//            }
//        }
//    }
//
//    private val bluetoothEnablingResult = registerForActivityResult(
//        ActivityResultContracts.StartActivityForResult()
//    ) { result ->
//        if (result.resultCode == Activity.RESULT_OK) {
//            Timber.i("Bluetooth is enabled, good to go")
//        } else {
//            Timber.e("User dismissed or denied Bluetooth prompt")
//            promptEnableBluetooth()
//        }
//    }

//    override fun onCreate(savedInstanceState: Bundle?) {
//        super.onCreate(savedInstanceState)
//        setContentView(R.layout.activity_point_graph)
//
//        lineChart = findViewById(R.id.lineChart)
//
//        // Get user location from intent - this was just trying to get the last rssi from mainactivity, now we have workerclass
////        val userX = intent.getDoubleExtra("USER_LOCATION_X", 0.0)
////        val userY = intent.getDoubleExtra("USER_LOCATION_Y", 0.0)
//
//        // Add initial user location to the graph
//        currentEntry = Entry(userX.toFloat(), userY.toFloat())
//        updateChart()
//
//        addButton.setOnClickListener {
//            addPoint()
//        }
//
//        // Start updating the user's position
//        startUpdatingPosition()
//    }


/**
 * Old code for adding user input points into graph
 */
//    private fun addPoint(int xValue, int yValue) {
//        // val xValue = xValueInput.text.toString().toFloatOrNull()
//        // val yValue = yValueInput.text.toString().toFloatOrNull()
//
//        if (xValue != null && yValue != null) {
//            currentEntry = Entry(xValue, yValue)
//            updateChart()
////            xValueInput.text.clear()
////            yValueInput.text.clear()
//        } else {
//            Toast.makeText(this, "Please enter valid values", Toast.LENGTH_SHORT).show()
//        }
//    }
//
//    private fun updateChart() {
//        val dataSet = LineDataSet(listOfNotNull(currentEntry), "Current Position") // Only show the current entry
//        dataSet.setDrawCircles(true) // Show circles for points
//        dataSet.setDrawValues(false) // Do not show values on the points
//        lineChart.data = LineData(dataSet)
//        lineChart.invalidate() // Refresh the chart
//    }
//
//    private fun startUpdatingPosition() {
//        updateRunnable = object : Runnable {
//            override fun run() {
//                // Retrieve the latest RSSI values and recalculate the user's position
//                val newUserLocation = getUpdatedUserLocation() // Implement this method
//                currentEntry = Entry(newUserLocation.first.toFloat(), newUserLocation.second.toFloat())
//                updateChart()
//                handler.postDelayed(this, 500) // Update every 500 milliseconds
//            }
//        }
//        handler.post(updateRunnable)
//    }
//
//    private fun getUpdatedUserLocation(): Pair<Double, Double> {
//        // Implement logic to get the latest RSSI values and recalculate the user's position
//        // This is a placeholder; you need to implement the actual logic
//        return Pair(0.0, 0.0) // Replace with actual calculation
//    }
//
//    override fun onDestroy() {
//        super.onDestroy()
//        handler.removeCallbacks(updateRunnable) // Stop updates when activity is destroyed
//    }

