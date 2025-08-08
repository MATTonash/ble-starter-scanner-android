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

class ScanResultAdapter(
    private var scanResults: List<ScanResult>,

    private val onClickListener: (device : ScanResult) -> Unit // Correctly reference the lambda here
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

//    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
//        val view = LayoutInflater.from(parent.context).inflate(
//            R.layout.row_scan_result,
//            parent,
//            false
//        )
//        return ViewHolder(view, onItemClick) // Pass the onItemClick lambda to the ViewHolder
//    }

    override fun getItemCount() = scanResults.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val scanResult = scanResults[position]
        holder.bind(scanResult)

        // Comment out or remove this part
        // holder.itemView.setOnClickListener {
        //     toggleSelection(scanResult)
        // }
    }

    fun getSelectedItems(): List<ScanResult> {
//        return selectedItems.toList()
        return emptyList()
    }


    class ViewHolder(
        private val view: View,
        private val onClickListener: (device : ScanResult) -> Unit
    ) : RecyclerView.ViewHolder(view) {
        private val beaconProjects = mapOf(
            "80:EC:CC:CD:33:28" to "Obstacle 1",
            "80:EC:CC:CD:33:7C" to "Happy Mornings (HM)",
            "80:EC:CC:CD:33:7E" to "Obstacle 2",
            "80:EC:CC:CD:33:58" to "Obstacle 3",
            "EC:81:F6:64:F0:86" to "Vision",
            "6C:B2:FD:35:01:6C" to "Tactile Display",
            "E0:35:2F:E6:42:46" to "GUIDE 1",
            "CB:31:FE:48:1B:CB" to "GUIDE 2",
            "D8:F2:C8:9B:33:34" to "Switch",
            "00:3C:84:28:87:01" to "MAP",
            "00:3C:84:28:77:AB" to "Dance",
            "F4:65:0B:40:5D:0E" to "Homemade Beacon"
        )

        // Beacon names
        // 1. "80:EC:CC:CD:33:28" - Beacon EW1 - Project 1
        // 2. "80:EC:CC:CD:33:7C" - Beacon EW2 - Project 2
        // 3. "80:EC:CC:CD:33:7E" - Beacon EW3 - Project 3
        // 4. "80:EC:CC:CD:33:58" - Beacon EW6 - Project 4
        // 5. "EC:81:F6:64:F0:86" - Beacon Apple 06 - Project 5
        // 6. "6C:B2:FD:35:01:6C" - Beeliner 03 - Project 6
        // 7. "E0:35:2F:E6:42:46" - Beacon Apple 04 - Project 7
        // 8. "CB:31:FE:48:1B:CB" - Beacon Apple 05 - Project 8
        // 9. "D8:F2:C8:9B:33:34" - RDL 04 - Project 9
        // 10. "00:3C:84:28:87:01" - RFstar-01 - Project 10
        // 11. "00:3C:84:28:77:AB" - RFstar-05 - Project 11

        @SuppressLint("MissingPermission", "SetTextI18n")
        fun bind(result: ScanResult) {
            view.findViewById<TextView>(R.id.device_name).text =
                if (view.context.hasRequiredBluetoothPermissions()) {
                    beaconProjects[result.device.address] ?: "Unknown Beacon"
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
                onClickListener(result) //Temporary removal of Item Click
                } //Use the onItemClick lambda
        }

    }
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

    fun updateList(newList: List<ScanResult>) {
        this.scanResults = newList
        notifyDataSetChanged()
    }
}
