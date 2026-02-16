package com.matt.guidebeacons.beacons

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.punchthrough.blestarterappandroid.databinding.RowBeaconDetailedBinding

class BeaconsAdapter(
    private val onClickListener: (beacon: Beacon) -> Unit
) : RecyclerView.Adapter<BeaconsAdapter.ViewHolder>() {

    private fun getBeacons(): List<Beacon> {
        return BeaconData.getBeaconProjects().values.toList()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val binding = RowBeaconDetailedBinding.inflate(inflater, parent, false)
        return ViewHolder(binding, onClickListener)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val beacons = getBeacons()[position]
        holder.bind(beacons)
    }

    override fun getItemCount() = getBeacons().size


    class ViewHolder(
        private val binding: RowBeaconDetailedBinding,
        private val onClickListener: (Beacon) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {

        @SuppressLint("SetTextI18n")
        fun bind(beacon: Beacon) {
            binding.deviceName.text = beacon.toString()
            binding.macAddress.text = BeaconData.getBeaconMacAddress(beacon)
            binding.beaconType.text = beacon.getBeaconType().toString()
            binding.coordinates.text = beacon.getCoordinatesString()
            binding.calibrationRssi.text = "${beacon.getCalibrationRSSI()} dBm"

            binding.root.setOnClickListener {
                timber.log.Timber.i("Clicked on beacon \"${beacon}\"")
                onClickListener.invoke(beacon)
            }
        }
    }
}