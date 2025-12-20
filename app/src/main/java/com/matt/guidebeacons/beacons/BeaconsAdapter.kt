package com.matt.guidebeacons.beacons

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.punchthrough.blestarterappandroid.databinding.RowScanResultBinding
import timber.log.Timber

class BeaconsAdapter(
    private val onClickListener: (beacon: Beacon) -> Unit
) : RecyclerView.Adapter<BeaconsAdapter.ViewHolder>() {

    private fun getBeacons(): List<Beacon> {
        return BeaconData.getBeaconProjects().values.toList()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        Timber.i("Listing ${itemCount} beacon(s)")
        val inflater = LayoutInflater.from(parent.context)
        val binding = RowScanResultBinding.inflate(inflater, parent, false)
        return ViewHolder(binding, onClickListener)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val beacons = getBeacons()[position]
        holder.bind(beacons)
    }

    override fun getItemCount() = getBeacons().size


    class ViewHolder(
        private val binding: RowScanResultBinding,
        private val onClickListener: (Beacon) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {

        @SuppressLint("SetTextI18n")
        fun bind(beacon: Beacon) {
            val view = binding.root

            binding.deviceName.text = beacon.toString()
            binding.macAddress.text = beacon.getCoordinatesString()
            binding.signalStrength.text = "${beacon.getCalibrationRSSI()} dBm"

            view.setOnClickListener {
                Timber.i("Clicked on beacon \"${beacon}\"")
                onClickListener.invoke(beacon)
            }
        }
    }
}