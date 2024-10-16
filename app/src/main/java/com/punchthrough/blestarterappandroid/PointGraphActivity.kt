package com.punchthrough.blestarterappandroid

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet

class PointGraphActivity : AppCompatActivity() {

    private lateinit var lineChart: LineChart
    private lateinit var addButton: Button
    private lateinit var xValueInput: EditText
    private lateinit var yValueInput: EditText
    private val handler = Handler(Looper.getMainLooper())
    private lateinit var updateRunnable: Runnable
    private var currentEntry: Entry? = null // Store the current entry

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_point_graph)

        lineChart = findViewById(R.id.lineChart)
        addButton = findViewById(R.id.addButton)
        xValueInput = findViewById(R.id.xValueInput)
        yValueInput = findViewById(R.id.yValueInput)

        // Get user location from intent
        val userX = intent.getDoubleExtra("USER_LOCATION_X", 0.0)
        val userY = intent.getDoubleExtra("USER_LOCATION_Y", 0.0)

        // Add initial user location to the graph
        currentEntry = Entry(userX.toFloat(), userY.toFloat())
        updateChart()

        addButton.setOnClickListener {
            addPoint()
        }

        // Start updating the user's position
        startUpdatingPosition()
    }

    private fun addPoint() {
        val xValue = xValueInput.text.toString().toFloatOrNull()
        val yValue = yValueInput.text.toString().toFloatOrNull()

        if (xValue != null && yValue != null) {
            currentEntry = Entry(xValue, yValue)
            updateChart()
            xValueInput.text.clear()
            yValueInput.text.clear()
        } else {
            Toast.makeText(this, "Please enter valid values", Toast.LENGTH_SHORT).show()
        }
    }

    private fun updateChart() {
        val dataSet = LineDataSet(listOfNotNull(currentEntry), "Current Position") // Only show the current entry
        dataSet.setDrawCircles(true) // Show circles for points
        dataSet.setDrawValues(false) // Do not show values on the points
        lineChart.data = LineData(dataSet)
        lineChart.invalidate() // Refresh the chart
    }

    private fun startUpdatingPosition() {
        updateRunnable = object : Runnable {
            override fun run() {
                // Retrieve the latest RSSI values and recalculate the user's position
                val newUserLocation = getUpdatedUserLocation() // Implement this method
                currentEntry = Entry(newUserLocation.first.toFloat(), newUserLocation.second.toFloat())
                updateChart()
                handler.postDelayed(this, 500) // Update every 500 milliseconds
            }
        }
        handler.post(updateRunnable)
    }

    private fun getUpdatedUserLocation(): Pair<Double, Double> {
        // Implement logic to get the latest RSSI values and recalculate the user's position
        // This is a placeholder; you need to implement the actual logic
        return Pair(0.0, 0.0) // Replace with actual calculation
    }

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacks(updateRunnable) // Stop updates when activity is destroyed
    }
}
