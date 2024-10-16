/*
 * Copyright 2024 Punch Through Design LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.punchthrough.blestarterappandroid

import android.annotation.SuppressLint
import android.bluetooth.le.ScanResult
import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class ScanResultAdapter(
    private var scanResults: List<ScanResult>,
    private val onItemClick: (ScanResult) -> Unit // Correctly reference the lambda here
) : RecyclerView.Adapter<ScanResultAdapter.ViewHolder>() {

    private val selectedItems = mutableSetOf<ScanResult>()

    // New method to get RSSI for a specific device address
    fun getRssiForDevice(address: String): Int? {
        return scanResults.find { it.device.address == address }?.rssi
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(
            R.layout.row_scan_result,
            parent,
            false
        )
        return ViewHolder(view, onItemClick) // Pass the onItemClick lambda to the ViewHolder
    }

    override fun getItemCount() = scanResults.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val scanResult = scanResults[position]
        holder.bind(scanResult)

        // Handle item click
        holder.itemView.setOnClickListener {
            if (selectedItems.contains(scanResult)) {
                selectedItems.remove(scanResult)
                holder.itemView.setBackgroundColor(Color.WHITE) // Deselect
            } else {
                if (selectedItems.size < 3) { // Limit selection to 3
                    selectedItems.add(scanResult)
                    holder.itemView.setBackgroundColor(Color.LTGRAY) // Select
                }
            }
            onItemClick(scanResult) // Invoke the onItemClick lambda
        }
    }

    fun getSelectedItems(): List<ScanResult> {
        return selectedItems.toList()
    }

    class ViewHolder(
        private val view: View,
        private val onItemClick: (ScanResult) -> Unit // Add this parameter
    ) : RecyclerView.ViewHolder(view) {

        @SuppressLint("MissingPermission", "SetTextI18n")
        fun bind(result: ScanResult) {
            view.findViewById<TextView>(R.id.device_name).text =
                if (view.context.hasRequiredBluetoothPermissions()) {
                    result.device.name ?: "Unnamed"
                } else {
                    error("Missing required Bluetooth permissions")
                }
            view.findViewById<TextView>(R.id.mac_address).text = result.device.address
            view.findViewById<TextView>(R.id.signal_strength).text = "${result.rssi} dBm"
            view.setOnClickListener { onItemClick(result) } // Use the onItemClick lambda
        }
    }

    fun updateList(newList: List<ScanResult>) {
        this.scanResults = newList
        notifyDataSetChanged()
    }
}
