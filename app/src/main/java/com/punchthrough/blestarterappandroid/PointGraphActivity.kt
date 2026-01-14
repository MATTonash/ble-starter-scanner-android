package com.punchthrough.blestarterappandroid

import android.bluetooth.le.ScanResult
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import com.github.mikephil.charting.charts.ScatterChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.ScatterData
import com.github.mikephil.charting.data.ScatterDataSet
import com.github.mikephil.charting.interfaces.datasets.IScatterDataSet
import com.matt.guidebeacons.beacons.BeaconData
import kotlin.math.pow


/**
 * The mapping of user position activity
 * For now hardcoding the beacon locations but later will implement this in a calibration stage
 * Please see doc for the layout
 *
 * To understand mapping methods we use the MPAndroidChart library
 * https://github.com/PhilJay/MPAndroidChart/wiki/Dynamic-&-Realtime-Data
 */
class PointGraphActivity : AppCompatActivity() {
    // private lateinit var lineChart: LineChart
    private lateinit var scatterChart: ScatterChart
    private val bluetoothWorker = BluetoothWorkerClass.getInstance()
    private var userPoints = ArrayList<Entry>()
    private var locationDataSet = ScatterDataSet(userPoints,"User position")

    private val beaconProjects = BeaconData.getBeaconProjects()
    private lateinit var trilaterationFunction : TrilaterationFunction


    // For when doing multiple data sets, doesn't work with dynamic updating :(
    // private var beaconLocations = ArrayList<Entry>()
    // private var beaconsDataSet = ScatterDataSet(beaconLocations, "Beacons")

//    var beacon1dist: Double = 0.0
//    var beacon2dist: Double = 0.0
//    var beacon3dist: Double = 0.0
//    private var trilaterationFunction = TrilaterationFunction(beacon1.getCoordinates(), beacon2.getCoordinates(), beacon1dist, beacon2dist)

    private var scatterDataSets = ArrayList<IScatterDataSet>()

    /**
     * Bluetooth worker instances are saved but reinitialised in each activity
     */
    @RequiresApi(Build.VERSION_CODES.O)
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
        scatterChart = findViewById(R.id.scatterChart)

        title = "User location"

        val scatterData = ScatterData()

        // Add the data sets to the chart data
        // scatterData.addDataSet(beaconsDataSet)
        scatterData.addDataSet(locationDataSet)

        // Set the data to the chart
        scatterChart.data = scatterData

        scatterChart.isDragEnabled = true
        scatterChart.setPinchZoom(true)
        scatterChart.setScaleEnabled(true)

        scatterChart.setBackgroundColor(Color.WHITE)
        val xAxis = scatterChart.xAxis
        xAxis.position = XAxis.XAxisPosition.BOTTOM
        xAxis.textColor = Color.BLACK
        xAxis.setDrawGridLines(true)
        xAxis.axisMinimum = -4f
        xAxis.axisMaximum = 4f

        val yAxisL = scatterChart.axisLeft
        yAxisL.textColor = Color.BLACK
        yAxisL.setDrawGridLines(true)
        yAxisL.axisMinimum = -4f
        yAxisL.axisMaximum = 4f

        val yAxisR = scatterChart.axisRight
        yAxisR.isEnabled = false

        locationDataSet.setScatterShape(ScatterChart.ScatterShape.TRIANGLE)
        locationDataSet.setColor(Color.BLUE)
        locationDataSet.setDrawValues(true)

//        beaconsDataSet.setScatterShape(ScatterChart.ScatterShape.CIRCLE)
//        beaconsDataSet.setColor(Color.BLACK)
//        beaconsDataSet.setDrawValues(false)
//
//        // This was to map beacons at the same time but this fucks up dynamic updating for some reason :(
//        beaconLocations.add(Entry(beacon1.getCoordinates()[0].toFloat(), beacon1.getCoordinates()[1].toFloat()))
//        beaconLocations.add(Entry(beacon2.getCoordinates()[0].toFloat(), beacon2.getCoordinates()[1].toFloat()))
//        beaconLocations.add(Entry(beacon3.getCoordinates()[0].toFloat(), beacon3.getCoordinates()[1].toFloat()))
//        beaconLocations.add(Entry(beacon4.getCoordinates()[0].toFloat(), beacon4.getCoordinates()[1].toFloat()))
//        beaconLocations.add(Entry(beacon5.getCoordinates()[0].toFloat(), beacon5.getCoordinates()[1].toFloat()))
//        beaconLocations.add(Entry(beacon6.getCoordinates()[0].toFloat(), beacon6.getCoordinates()[1].toFloat()))
//
//        beaconsDataSet.notifyDataSetChanged()

        // Initial chart update
        updateChartData()
    }

    private fun updateChartData() {
        val locationDataSet = ScatterDataSet(userPoints, "User position").apply {
            setScatterShape(ScatterChart.ScatterShape.TRIANGLE)
            color = Color.BLUE
            setDrawIcons(true)
            setDrawValues(true)
            scatterShapeSize = 20f
        }

        val scatterData = scatterChart.scatterData ?: ScatterData()
        if (scatterData.dataSetCount > 0) {
            scatterData.removeDataSet(0)
        }
        scatterData.addDataSet(locationDataSet)

        scatterChart.data = scatterData

        // Notify the chart that data has changed
        scatterChart.data.notifyDataChanged()
        scatterChart.notifyDataSetChanged()

        // Force the chart to redraw
        scatterChart.invalidate()
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

        // Create solver with current beacons and set distances
        trilaterationFunction = TrilaterationFunction(coords)
        trilaterationFunction.setBeaconDistances(distances)

        val coordinates = trilaterationFunction.solve()

        userPoints.clear()
        userPoints.add(Entry(coordinates[0].toFloat(), coordinates[1].toFloat()))

        updateChartData()
    }

    /**
     * In the scenario that the app is only able to detect 2 beacons, we have implemented trilateration equations
     * using only 2 coordinates and sets of distances
     *
     * NOTE: There are 3 equations here, still testing which is the most accurate
     */
    private fun trilaterate2D(beacon1: DoubleArray, beacon2: DoubleArray, beacon3: DoubleArray, beacon1dist: Double, beacon2dist: Double, beacon3dist: Double): DoubleArray {

//        val A = 2 * (beacon2[0] - beacon1[0])
//        val B = 2 * (beacon2[1] - beacon1[1])
//        val C = beacon1dist.pow(2) - beacon2dist.pow(2) - beacon1[0].pow(2) - beacon1[1].pow(2) + beacon2[0].pow(2) + beacon2[1].pow(2)
//        val D = 2 * (beacon3[0] - beacon2[0])
//        val E = 2 * (beacon3[1] - beacon2[1])
//        val F = beacon2dist.pow(2) - beacon3dist.pow(2) - beacon2[0].pow(2) - beacon2[1].pow(2) + beacon3[0].pow(2) + beacon3[1].pow(2)
//
//        val x = (C * E - F * B) / (E * A - B * D)
//        val y = (C * D - A * F) / (B * D - A * E)
//
//        return doubleArrayOf(x, y)


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


        // another way (stackexchange)
        val y = (1 - beacon3dist.pow(2) + beacon1dist.pow(2))/2
        val x = (beacon2dist.pow(2) - beacon3dist.pow(2) + 2*y)/2

        return doubleArrayOf(x, y)
    }

    @RequiresApi(Build.VERSION_CODES.O)
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