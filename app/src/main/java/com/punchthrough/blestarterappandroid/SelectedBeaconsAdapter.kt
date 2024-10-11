package com.punchthrough.blestarterappandroid

import android.annotation.SuppressLint
import android.bluetooth.le.ScanResult
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class SelectedBeaconsAdapter(
    private val selectedBeacons: List<ScanResult>
) : RecyclerView.Adapter<SelectedBeaconsAdapter.ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(
            R.layout.row_selected_beacon, // Create this layout file for displaying each selected beacon
            parent,
            false
        )
        return ViewHolder(view)
    }

    override fun getItemCount() = selectedBeacons.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val scanResult = selectedBeacons[position]
        holder.bind(scanResult)
    }

    class ViewHolder(private val view: View) : RecyclerView.ViewHolder(view) {
        @SuppressLint("MissingPermission")
        fun bind(result: ScanResult) {
            view.findViewById<TextView>(R.id.device_name).text =
                result.device.name ?: "Unnamed"
            view.findViewById<TextView>(R.id.mac_address).text = result.device.address
            view.findViewById<TextView>(R.id.signal_strength).text = "${result.rssi} dBm"
        }
    }
}