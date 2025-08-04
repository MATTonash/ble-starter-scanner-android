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
    private val dataPoints = ArrayList<Entry>()

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

    var beacon1 = doubleArrayOf(0.0, 0.0)
    var beacon2 = doubleArrayOf(0.0, 1.0)
    var beacon3 = doubleArrayOf(1.0, 0.0)

    var beacon1dist: Double = 0.0
    var beacon2dist: Double = 0.0
    var beacon3dist: Double = 0.0
    private var trilaterationFunction = TrilaterationFunction(beacon1, beacon2, beacon1dist, beacon2dist)


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
        val dataSet = LineDataSet(dataPoints, "User position").apply {
            color = Color.BLUE
            setCircleColor(Color.BLUE)
            setDrawCircles(true)
            setDrawValues(false)
            lineWidth = 2f
            circleRadius = 4f
            mode = LineDataSet.Mode.LINEAR
            lineChart.setTouchEnabled(true)
            lineChart.setPinchZoom(true)
        }

        lineChart.data = LineData(dataSet)

        lineChart.invalidate() // Refresh chart
    }

    private fun startRssiTracking() {
        bluetoothWorker.startScanning(
            callback = { results ->
                if (results.size > 2) {
                    handleScanResults(results)
                }
            },
            continuous = true,
            period = 500L,    // Scan every second
            interval = 50L    // Small interval between scans
        )
    }



    private fun handleScanResults(rawResults: List<ScanResult>) {
        val results = rawResults.sortedByDescending { it.rssi }

        if (results.size == 2) {
            trilaterationFunction.setBeacon1Dist(bluetoothWorker.rssiToDistance(results[0].rssi))
            trilaterationFunction.setBeacon2Dist(bluetoothWorker.rssiToDistance(results[1].rssi))
            val coordinates = trilaterationFunction.solve()
            dataPoints.clear()
            dataPoints.add(Entry(coordinates[0].toFloat(), coordinates[1].toFloat()))
        }


        else {
            val coordinates = trilaterate2D(beacon1, beacon2, beacon3, beacon1dist, beacon2dist, beacon3dist)
            dataPoints.clear()
            dataPoints.add(Entry(coordinates[0].toFloat(), coordinates[1].toFloat()))
        }

        updateChartData()
    }

    private fun trilaterate2D(beacon1: DoubleArray, beacon2: DoubleArray, beacon3: DoubleArray, beacon1dist: Double, beacon2dist: Double, beacon3dist: Double): DoubleArray {
        val A = 2 * (beacon2[0] - beacon1[0])
        val B = 2 * (beacon2[1] - beacon1[1])
        val C = beacon1dist.pow(2) - beacon2dist.pow(2) - beacon1[0].pow(2) - beacon1[1].pow(2) + beacon2[0].pow(2) + beacon2[1].pow(2)
        val D = 2 * (beacon3[0] - beacon2[0])
        val E = 2 * (beacon3[1] - beacon2[1])
        val F = beacon2dist.pow(2) - beacon3dist.pow(2) - beacon2[0].pow(2) - beacon2[1].pow(2) + beacon3[0].pow(2) + beacon3[1].pow(2)

        val x = (C * E - F * B) / (E * A - B * D)
        val y = (C * D - A * F) / (B * D - A * E)

        return doubleArrayOf(x, y)
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