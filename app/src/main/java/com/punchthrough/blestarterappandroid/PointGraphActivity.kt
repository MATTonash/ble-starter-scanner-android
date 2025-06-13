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


    private val radii = mutableMapOf<String, Float>()
    var deviceAddressList = ArrayList<String>()


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val targetDeviceAddress1 = intent.getStringExtra("TARGET_DEVICE_ADDRESS_1")
        val targetDeviceAddress2 = intent.getStringExtra("TARGET_DEVICE_ADDRESS_2")
        val targetDeviceAddress3 = intent.getStringExtra("TARGET_DEVICE_ADDRESS_3")
        if (targetDeviceAddress1 != null) {
            deviceAddressList.add(targetDeviceAddress1)
        }
        if (targetDeviceAddress2 != null) {
            deviceAddressList.add(targetDeviceAddress2)
        }
        if (targetDeviceAddress3 != null) {
            deviceAddressList.add(targetDeviceAddress3)
        }


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
        lineChart.invalidate() // Refresh chart
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

        val x1 = 0f
        val y1 = 0f
        val x2 = 1f
        val y2 = 0f
        val x3 = 0f
        val y3 = 1f
        val rFloatList = ArrayList<Float>()
        // Check if we've established connection (logic in workerclass 178)
//        for (bluetoothDevice in ConnectionManager.deviceGattMap.keys()) {
//            for (result in results) {
//                if (result.device == bluetoothDevice) {
//                    radii[bluetoothDevice.address] = result.rssi.toFloat()
//                    rFloatList.add(result.rssi.toFloat())
//                }
//            }
//        }

//        for (scanResult in bluetoothWorker.getCurrentResults()) {
//            for (result in results) {
//                if (result == scanResult) {
//                    radii[scanResult.device.address] = result.rssi.toFloat()
//                    rFloatList.add(result.rssi.toFloat())
//                }
//            }
//        }

        for (scanResult in results) {
            radii[scanResult.device.address] = scanResult.rssi.toFloat()
            rFloatList.add(scanResult.rssi.toFloat())
        }
        if (rFloatList.size < 3) {
            return
        }
        trilaterate(x1, y1, rFloatList[0], x2, y2,rFloatList[1], x3, y3,rFloatList[2])


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

        dataPoints.add(Entry(x,y))

        if (dataPoints.size > 60) {
                dataPoints.removeAt(0)
                // Shift x-values
                dataPoints.forEachIndexed { index, entry ->
                    dataPoints[index] = Entry(index.toFloat(), entry.y)
                }
            }
        // dataPoints.clear()

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