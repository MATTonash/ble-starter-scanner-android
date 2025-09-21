package com.matt.guidebeacons.beacons

import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.punchthrough.blestarterappandroid.BluetoothWorkerClass

class BeaconsAdapter : RecyclerView.Adapter<BeaconsAdapter.ViewHolder>() {

    private val beacons = BluetoothWorkerClass.getInstance().getBeaconProjects()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        TODO("Not yet implemented")
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        TODO("Not yet implemented")
    }

    override fun getItemCount() = beacons.size

    class ViewHolder(
        private val view: View
    ) : RecyclerView.ViewHolder(view) {

    }
}