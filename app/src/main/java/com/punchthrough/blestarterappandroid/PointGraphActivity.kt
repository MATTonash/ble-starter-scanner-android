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

class PointGraphActivity : AppCompatActivity() {
    private lateinit var lineChart: LineChart
    private val bluetoothWorker = BluetoothWorkerClass.getInstance()
    private val dataPoints = mutableListOf<Entry>()
    private var timeCounter = 0f

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
            setDrawGridLines(true)
            setDrawAxisLine(true)
        }

        // Configure Y axis
        lineChart.axisLeft.apply {
            textColor = Color.BLACK
            setDrawGridLines(true)

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
        lineChart.invalidate() // Refresh chart
    }

    private fun startRssiTracking() {
        bluetoothWorker.startScanning(
            callback = { results ->
                handleScanResults(results)
            },
            continuous = true,
            period = 1000L,    // Scan every second
            interval = 500L    // Small interval between scans
        )
    }

    private fun handleScanResults(results: List<ScanResult>) {
        // Get the target device's RSSI
        // You might want to filter for a specific device address
        val targetDeviceAddress = intent.getStringExtra("TARGET_DEVICE_ADDRESS")

        results.find { it.device.address == targetDeviceAddress }?.let { result ->
            // Add new data point
            dataPoints.add(Entry(timeCounter, result.rssi.toFloat()))

            // Keep only last 60 seconds of data
            if (dataPoints.size > 60) {
                dataPoints.removeAt(0)
                // Shift x-values
                dataPoints.forEachIndexed { index, entry ->
                    dataPoints[index] = Entry(index.toFloat(), entry.y)
                }
            }

            // Update counter
            timeCounter++

            // Update chart
            updateChartData()
        }
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