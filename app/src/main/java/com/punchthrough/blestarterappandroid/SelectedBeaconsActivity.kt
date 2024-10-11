package com.punchthrough.blestarterappandroid

import android.bluetooth.le.ScanResult
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.punchthrough.blestarterappandroid.databinding.ActivitySelectedBeaconsBinding

class SelectedBeaconsActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySelectedBeaconsBinding
    private lateinit var selectedBeaconsAdapter: SelectedBeaconsAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySelectedBeaconsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val selectedBeacons = intent.getParcelableArrayListExtra<ScanResult>("SELECTED_BEACONS")

        selectedBeaconsAdapter = SelectedBeaconsAdapter(selectedBeacons ?: emptyList())
        binding.selectedBeaconsRecyclerView.apply {
            adapter = selectedBeaconsAdapter
            layoutManager = LinearLayoutManager(this@SelectedBeaconsActivity)
        }
    }
}