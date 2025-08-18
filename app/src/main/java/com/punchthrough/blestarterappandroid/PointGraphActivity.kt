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


    private val beaconProjects = bluetoothWorker.getBeaconProjects()
    private val beacons = beaconProjects.values.toList()
    var beacon1 = beacons[0]
    var beacon2 = beacons[1]
    var beacon3 = beacons[2]
    var beacon4 = beacons[3]
    var beacon5 = beacons[4]
    var beacon6 = beacons[5]

    var beacon1dist: Double = 0.0
    var beacon2dist: Double = 0.0
    var beacon3dist: Double = 0.0
    private var trilaterationFunction = TrilaterationFunction(beacon1.getCoordinates(), beacon2.getCoordinates(), beacon1dist, beacon2dist)


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_point_graph)

        // Initialize Bluetooth Worker
        bluetoothWorker.initialize(this)

        // Setup chart
        setupChart()

        // Start scanning with custom parameters for graphing
        //startRssiTracking()
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
            axisMaximum = 5f
            axisMinimum = -5f
            setDrawGridLines(true)
            setDrawAxisLine(true)
        }

        // Configure Y axis
        lineChart.axisLeft.apply {
            textColor = Color.BLACK
            setDrawGridLines(true)
            axisMaximum = 5f
            axisMinimum = -5f
            setDrawAxisLine(true)
        }

        val dataSet = LineDataSet(dataPoints, "User position").apply {
            color = Color.BLUE
            setCircleColor(Color.BLUE)
            setDrawCircles(true)
            setDrawValues(false)
            lineWidth = 2f
            circleRadius = 4f
            mode = LineDataSet.Mode.LINEAR
        }

        lineChart.setTouchEnabled(true)
        lineChart.setPinchZoom(true)

        dataPoints.add(Entry(beacon1.getCoordinates()[0].toFloat(), beacon1.getCoordinates()[1].toFloat()))
        dataPoints.add(Entry(beacon2.getCoordinates()[0].toFloat(), beacon2.getCoordinates()[1].toFloat()))
        dataPoints.add(Entry(beacon3.getCoordinates()[0].toFloat(), beacon3.getCoordinates()[1].toFloat()))
        dataPoints.add(Entry(beacon4.getCoordinates()[0].toFloat(), beacon4.getCoordinates()[1].toFloat()))
        dataPoints.add(Entry(beacon5.getCoordinates()[0].toFloat(), beacon5.getCoordinates()[1].toFloat()))
        dataPoints.add(Entry(beacon6.getCoordinates()[0].toFloat(), beacon6.getCoordinates()[1].toFloat()))

        lineChart.data = LineData(dataSet)
//
//        // Configure right Y axis
//        lineChart.axisRight.isEnabled = false

        // Initialize data
        updateChartData()
    }

    private fun updateChartData() {
        lineChart.data.notifyDataChanged()
        lineChart.notifyDataSetChanged()
        lineChart.invalidate() // Refresh chart
    }

    private fun startRssiTracking() {
        bluetoothWorker.startScanning(
            callback = { results ->
                handleScanResults(results)
            },
            continuous = true,
            period = 5000L,    // Scan every second
            interval = 2000L    // Small interval between scans
        )
    }



    private fun handleScanResults(rawResults: List<ScanResult>) {
        val results = rawResults.sortedByDescending { it.rssi }

        if (results.size < 2) {
            return
        }

        if (results.size == 2) {
            beacon1dist = (beaconProjects[results[0].device.address]?.calculateDistance(results[0].rssi, results[0].txPower.toDouble())
                ?: trilaterationFunction.setBeacon1Dist(beacon1dist )) as Double
            beacon2dist = (beaconProjects[results[1].device.address]?.calculateDistance(results[1].rssi, results[1].txPower.toDouble())
                ?: trilaterationFunction.setBeacon2Dist(beacon2dist )) as Double
            trilaterationFunction.setBeacon2Dist(beacon2dist)
            val coordinates = trilaterationFunction.solve()

            dataPoints.removeLast()
            dataPoints.add(Entry(coordinates[0].toFloat(), coordinates[1].toFloat()))
        }


        else {
            beacon1dist = (beaconProjects[results[0].device.address]?.calculateDistance(results[0].rssi, results[0].txPower.toDouble())
                ?: trilaterationFunction.setBeacon1Dist(beacon1dist )) as Double
            beacon2dist = (beaconProjects[results[1].device.address]?.calculateDistance(results[1].rssi, results[1].txPower.toDouble())
                ?: trilaterationFunction.setBeacon1Dist(beacon1dist )) as Double
            beacon3dist = (beaconProjects[results[2].device.address]?.calculateDistance(results[2].rssi, results[2].txPower.toDouble())
                ?: trilaterationFunction.setBeacon1Dist(beacon3dist )) as Double
            val coordinates = trilaterate2D(beacon1.getCoordinates(), beacon2.getCoordinates(), beacon3.getCoordinates(), beacon1dist, beacon2dist, beacon3dist)
            dataPoints.removeLast()
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

//        // The separation of beacons 1 and 2
//        // Distance formula
//        val u = sqrt((beacon1[0] - beacon2[0]).pow(2) + (beacon1[1] - beacon2[1]).pow(2))
//
//        val x = (beacon1dist.pow(2)-beacon2dist.pow(2) + u.pow(2))/(2*u)
//        val negativeY = -sqrt(beacon1dist.pow(2) - x.pow(2))
//        val positiveY = sqrt(beacon1dist.pow(2) - x.pow(2))
//
//        // See which potential y value is closest to the third beacon
//        val negGap = sqrt((beacon3[0] - x).pow(2) + (beacon3[1] - negativeY).pow(2))
//        val posGap = sqrt((beacon3[0] - x).pow(2) + (beacon3[1] - positiveY).pow(2))
//        if (negGap <= posGap || posGap.isNaN()) {
//            return doubleArrayOf(x, negativeY)
//        } else if (negGap.isNaN()) {
//            return doubleArrayOf(x, positiveY)
//        }
//        return doubleArrayOf(x, positiveY)
    }

    override fun onResume() {
        super.onResume()
        //startRssiTracking()
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