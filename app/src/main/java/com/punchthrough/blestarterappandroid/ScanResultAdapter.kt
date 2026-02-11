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
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.matt.guidebeacons.beacons.BeaconData

/**
* The adapter for recycler view of all the beacons/scan results in mainActivity
 **/
class ScanResultAdapter(
    private var scanResults: List<ScanResult>,

    private var listener: View.OnClickListener? = null,

    private var onClickListener: (device : ScanResult) -> Unit // Correctly reference the lambda here
) : RecyclerView.Adapter<ScanResultAdapter.ViewHolder>() {

    // New method to get RSSI for a specific device address
    fun getRssiForDevice(address: String): Int? {
        return scanResults.find { it.device.address == address }?.rssi
    }

    // create new views
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        // inflates the card_view_design view
        // that is used to hold list item
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.row_scan_result, parent, false)

        return ViewHolder(view, onClickListener)
    }

    override fun getItemCount() = scanResults.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val scanResult = scanResults[position]
        holder.bind(scanResult)
        // Set click listener for the item view
    }


    class ViewHolder(
        private val view: View,
        private val onClickListener: (device : ScanResult) -> Unit

    ) : RecyclerView.ViewHolder(view) {

        private val beaconProjects = BeaconData.getBeaconProjects()

        @SuppressLint("MissingPermission", "SetTextI18n")
        fun bind(result: ScanResult) {
            view.findViewById<TextView>(R.id.device_name).text =
                if (view.context.hasRequiredBluetoothPermissions()) {
                    beaconProjects[result.device.address].toString() ?: "Unknown Beacon"
                } else {
                    error("Missing required Bluetooth permissions")
                }
            view.findViewById<TextView>(R.id.mac_address).text = result.device.address
            view.findViewById<TextView>(R.id.signal_strength).text =
                if (result.rssi < -65 && result.rssi > -80) {
                    "Far (" +  result.rssi.toString() + " dBm)"
                } else if(result.rssi < -80){
                    "Very Far (" +  result.rssi.toString() + " dBm)"
                } else {
                    "Near (" +  result.rssi.toString() + " dBm)"
                }

            view.setOnClickListener {
                onClickListener.invoke(result) //Temporary removal of Item Click
                } //Use the onItemClick lambda
        }

    }

    fun updateList(newList: List<ScanResult>) {
        this.scanResults = newList
        notifyDataSetChanged()
    }
}
