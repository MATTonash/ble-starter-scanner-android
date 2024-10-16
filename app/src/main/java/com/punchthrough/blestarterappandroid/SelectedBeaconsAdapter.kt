package com.punchthrough.blestarterappandroid

import android.bluetooth.le.ScanResult
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class SelectedBeaconsAdapter(
    private val selectedBeacons: List<ScanResult>
) : RecyclerView.Adapter<SelectedBeaconsAdapter.ViewHolder>() {

    private val coordinatesMap = mutableMapOf<String, Pair<Double, Double>>() // Store coordinates

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(
            R.layout.row_selected_beacon,
            parent,
            false
        )
        return ViewHolder(view)
    }

    override fun getItemCount() = selectedBeacons.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val scanResult = selectedBeacons[position]
        holder.bind(scanResult)

        // Store coordinates when the user inputs them
        holder.xCoordinateEditText.setOnFocusChangeListener { _, hasFocus ->
            if (!hasFocus) {
                val xValue = holder.xCoordinateEditText.text.toString().toDoubleOrNull()
                val yValue = holder.yCoordinateEditText.text.toString().toDoubleOrNull()
                if (xValue != null && yValue != null) {
                    coordinatesMap[scanResult.device.address] = Pair(xValue, yValue)
                }
            }
        }

        holder.yCoordinateEditText.setOnFocusChangeListener { _, hasFocus ->
            if (!hasFocus) {
                val xValue = holder.xCoordinateEditText.text.toString().toDoubleOrNull()
                val yValue = holder.yCoordinateEditText.text.toString().toDoubleOrNull()
                if (xValue != null && yValue != null) {
                    coordinatesMap[scanResult.device.address] = Pair(xValue, yValue)
                }
            }
        }
    }

    // New method to get RSSI for a specific device address
    fun getRssiForDevice(address: String): Int? {
        return selectedBeacons.find { it.device.address == address }?.rssi
    }

    fun setCoordinates(address: String, x: Double, y: Double) {
        coordinatesMap[address] = Pair(x, y)
    }

    fun getCoordinates(): Map<String, Pair<Double, Double>> {
        return coordinatesMap
    }

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val deviceNameTextView: TextView = view.findViewById(R.id.device_name)
        private val macAddressTextView: TextView = view.findViewById(R.id.mac_address)
        private val signalStrengthTextView: TextView = view.findViewById(R.id.signal_strength)
        val xCoordinateEditText: EditText = view.findViewById(R.id.x_coordinate)
        val yCoordinateEditText: EditText = view.findViewById(R.id.y_coordinate)

        fun bind(result: ScanResult) {
            deviceNameTextView.text = result.device.name ?: "Unnamed"
            macAddressTextView.text = result.device.address
            signalStrengthTextView.text = "${result.rssi} dBm"
        }
    }
}
