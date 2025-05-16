package com.punchthrough.blestarterappandroid

import android.bluetooth.le.ScanResult
import android.graphics.Color
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import kotlin.math.pow

class PointGraphActivity : AppCompatActivity() {
    private lateinit var lineChart: LineChart
    private val bluetoothWorker = BluetoothWorkerClass.getInstance()
    private val dataPoints = mutableListOf<Entry>()
    private var timeCounter = 0f
    private lateinit var deviceAddress: String
    private lateinit var deviceName: String
    // Get the target device's RSSI
    // You might want to filter for a specific device address
    private val beaconProjects = mapOf(
        "80:EC:CC:CD:33:28" to "Losing Things (LT)",
        "80:EC:CC:CD:33:7C" to "Happy Mornings (HM)",
        "80:EC:CC:CD:33:7E" to "STEM",
        "80:EC:CC:CD:33:58" to "Visual Clutter",
        "EC:81:F6:64:F0:86" to "Vision",
        "6C:B2:FD:35:01:6C" to "Tactile Display",
        "E0:35:2F:E6:42:46" to "GUIDE 1",
        "CB:31:FE:48:1B:CB" to "GUIDE 2",
        "D8:F2:C8:9B:33:34" to "Switch",
        "00:3C:84:28:87:01" to "MAP",
        "00:3C:84:28:77:AB" to "Dance"
    )

    private val radii = mutableMapOf<String, Float>()


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_point_graph)

        // Initialize Bluetooth Worker
        bluetoothWorker.initialize(this)

        // Setup chart
        setupChart()

        // Start scanning with custom parameters for graphing
        startRssiTracking()
    }

    private fun setupChart() {
        lineChart = findViewById(R.id.lineChart)

        // Configure chart appearance
        lineChart.apply {
            description.isEnabled = false
            setTouchEnabled(true)
            isDragEnabled = true
            setScaleEnabled(true)
            setPinchZoom(true)
            setBackgroundColor(Color.WHITE)
        }

        // Configure X axis
        lineChart.xAxis.apply {
            position = XAxis.XAxisPosition.BOTTOM
            textColor = Color.BLACK
            axisMaximum = 2f
            axisMinimum = -2f
            setDrawGridLines(true)
            setDrawAxisLine(true)
        }

        // Configure Y axis
        lineChart.axisLeft.apply {
            textColor = Color.BLACK
            setDrawGridLines(true)
            axisMaximum = 2f
            axisMinimum = -2f
            setDrawAxisLine(true)
        }

        // Configure right Y axis
        lineChart.axisRight.isEnabled = false

        // Initialize empty data
        updateChartData()
    }

    private fun updateChartData() {
        val dataSet = LineDataSet(dataPoints, "RSSI Values").apply {
            color = Color.BLUE
            setCircleColor(Color.BLUE)
            setDrawCircles(true)
            setDrawValues(false)
            lineWidth = 2f
            circleRadius = 4f
            mode = LineDataSet.Mode.LINEAR
        }

        lineChart.data = LineData(dataSet)
        // lineChart.invalidate() // Refresh chart
    }

    private fun startRssiTracking() {
        bluetoothWorker.startScanning(
            callback = { results ->
                handleScanResults(results)
            },
            continuous = true,
            period = 2000L,    // Scan every second
            interval = 500L    // Small interval between scans
        )
    }



    private fun handleScanResults(results: List<ScanResult>) {
        val targetDeviceAddress1 = intent.getStringExtra("TARGET_DEVICE_ADDRESS_1")
        val targetDeviceAddress2 = intent.getStringExtra("TARGET_DEVICE_ADDRESS_2")
        val targetDeviceAddress3 = intent.getStringExtra("TARGET_DEVICE_ADDRESS_3")
        val deviceAddressList = listOf(targetDeviceAddress1, targetDeviceAddress2, targetDeviceAddress3)
        val x1 = 0f
        val y1 = 0f
        val x2 = 1f
        val y2 = 0f
        val x3 = 0f
        val y3 = 1f
        for (address in deviceAddressList) {

            results.find { it.device.address == address }?.let { result ->
                radii[address.toString()] = bluetoothWorker.rssiToDistance(result.rssi).toFloat()
            }
        }
        if (radii.size == 3) {
            trilaterate(x1, y1, radii[targetDeviceAddress1.toString()]!!.toFloat(), x2, y2,
                radii[targetDeviceAddress2.toString()]!!.toFloat(), x3, y3,
                radii[targetDeviceAddress1.toString()]!!.toFloat())
        } else {
            startRssiTracking()
        }


//        results.find { it.device.address == targetDeviceAddress }?.let { result ->
//            // Add new data point
//            dataPoints.add(Entry(timeCounter,
//                bluetoothWorker.rssiToDistance(result.rssi.toFloat()).toFloat()
//            ))
//
//            // Keep only last 60 seconds of data
//            if (dataPoints.size > 60) {
//                dataPoints.removeAt(0)
//                // Shift x-values
//                dataPoints.forEachIndexed { index, entry ->
//                    dataPoints[index] = Entry(index.toFloat(), entry.y)
//                }
//            }
//
//            // Update counter
//            timeCounter++
//
//            // Update chart
//            updateChartData()
//        }
    }
    fun trilaterate(x1: Float, y1: Float, r1: Float,
                    x2: Float, y2: Float, r2: Float,
                    x3: Float, y3: Float, r3: Float){
        val A = 2 * (x2 - x1)
        val B = 2 * (y2 - y1)
        val C = r1.pow(2) - r2.pow(2) - x1.pow(2) - y1.pow(2) + x2.pow(2) + y2.pow(2)
        val D = 2 * (x3 - x2)
        val E = 2 * (y3 - y2)
        val F = r2.pow(2) - r3.pow(2) - x2.pow(2) - y2.pow(2) + x3.pow(2) + y3.pow(2)

        val x = (C * E - F * B) / (E * A - B * D)
        val y = (C * D - A * F) / (B * D - A * E)

        // dataPoints.clear()
        dataPoints.add(Entry(x,y))
        updateChartData()
    }

    override fun onResume() {
        super.onResume()
        if (!bluetoothWorker.isScanning()) {
            startRssiTracking()
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