package com.punchthrough.blestarterappandroid

import android.os.Bundle
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
    private val entries = mutableListOf<Entry>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_point_graph)

        lineChart = findViewById(R.id.lineChart)
        addButton = findViewById(R.id.addButton)
        xValueInput = findViewById(R.id.xValueInput)
        yValueInput = findViewById(R.id.yValueInput)

        addButton.setOnClickListener {
            addPoint()
        }
    }

    private fun addPoint() {
        val xValue = xValueInput.text.toString().toFloatOrNull()
        val yValue = yValueInput.text.toString().toFloatOrNull()

        if (xValue != null && yValue != null) {
            entries.add(Entry(xValue, yValue))
            updateChart()
            xValueInput.text.clear()
            yValueInput.text.clear()
        } else {
            Toast.makeText(this, "Please enter valid values", Toast.LENGTH_SHORT).show()
        }
    }

    private fun updateChart() {
        val dataSet = LineDataSet(entries, "Points")
        val lineData = LineData(dataSet)
        lineChart.data = lineData
        lineChart.invalidate() // Refresh the chart
    }
}