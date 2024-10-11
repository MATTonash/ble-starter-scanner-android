package com.punchthrough.blestarterappandroid

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import kotlin.math.pow
import kotlin.math.sqrt

class trilateration : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_trilateration)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val calculateButton: Button = findViewById(R.id.buttonCalculate)
        val resultText: TextView = findViewById(R.id.textViewResult)

        calculateButton.setOnClickListener {
            val x1 = findViewById<EditText>(R.id.editTextX1).text.toString().toDoubleOrNull() ?: 0.0
            val y1 = findViewById<EditText>(R.id.editTextY1).text.toString().toDoubleOrNull() ?: 0.0
            val r1 = findViewById<EditText>(R.id.editTextR1).text.toString().toDoubleOrNull() ?: 0.0
            val x2 = findViewById<EditText>(R.id.editTextX2).text.toString().toDoubleOrNull() ?: 0.0
            val y2 = findViewById<EditText>(R.id.editTextY2).text.toString().toDoubleOrNull() ?: 0.0
            val r2 = findViewById<EditText>(R.id.editTextR2).text.toString().toDoubleOrNull() ?: 0.0
            val x3 = findViewById<EditText>(R.id.editTextX3).text.toString().toDoubleOrNull() ?: 0.0
            val y3 = findViewById<EditText>(R.id.editTextY3).text.toString().toDoubleOrNull() ?: 0.0
            val r3 = findViewById<EditText>(R.id.editTextR3).text.toString().toDoubleOrNull() ?: 0.0

            val result = trilaterate(x1, y1, r1, x2, y2, r2, x3, y3, r3)
            resultText.text = "Intersection point: (${result.first}, ${result.second})"
        }
    }

    private fun trilaterate(x1: Double, y1: Double, r1: Double,
                            x2: Double, y2: Double, r2: Double,
                            x3: Double, y3: Double, r3: Double): Pair<Double, Double> {
        val A = 2 * (x2 - x1)
        val B = 2 * (y2 - y1)
        val C = r1.pow(2) - r2.pow(2) - x1.pow(2) - y1.pow(2) + x2.pow(2) + y2.pow(2)
        val D = 2 * (x3 - x2)
        val E = 2 * (y3 - y2)
        val F = r2.pow(2) - r3.pow(2) - x2.pow(2) - y2.pow(2) + x3.pow(2) + y3.pow(2)

        val x = (C * E - F * B) / (E * A - B * D)
        val y = (C * D - A * F) / (B * D - A * E)

        return Pair(x, y)
    }
}