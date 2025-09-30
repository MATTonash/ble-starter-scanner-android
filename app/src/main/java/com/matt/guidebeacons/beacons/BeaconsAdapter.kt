package com.matt.guidebeacons.beacons

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.punchthrough.blestarterappandroid.BluetoothWorkerClass
import com.punchthrough.blestarterappandroid.R
import timber.log.Timber

class BeaconsAdapter(
    private val onClickListener: (beacon: Beacon) -> Unit
) : RecyclerView.Adapter<BeaconsAdapter.ViewHolder>() {

    private val beacons = BluetoothWorkerClass.getInstance().getBeaconProjects().values.toList()


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        Timber.i("Listing ${itemCount} beacons")
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.row_scan_result, parent, false)

        return ViewHolder(view, onClickListener)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val beacon = beacons[position]
        holder.bind(beacon)
    }

    override fun getItemCount() = beacons.size


    class ViewHolder(
        private val view: View,
        private val onClickListener: (Beacon) -> Unit
    ) : RecyclerView.ViewHolder(view) {

        @SuppressLint("SetTextI18n")
        fun bind(beacon: Beacon) {
            view.findViewById<TextView>(R.id.device_name).text = beacon.toString()
            view.findViewById<TextView>(R.id.mac_address).text = beacon.getCoordinatesString()
            view.findViewById<TextView>(R.id.signal_strength).text = "${beacon.getCalibrationRSSI().toString()} dBm"

            view.setOnClickListener {
                Timber.i("Clicked on beacon ${beacon}")
                onClickListener.invoke(beacon)
            }
        }
    }
}