package com.matt.guidebeacons.activities

import android.os.Bundle
import androidx.annotation.UiThread
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.SimpleItemAnimator
import com.matt.guidebeacons.beacons.BeaconsAdapter
import com.punchthrough.blestarterappandroid.databinding.ActivityCalibrationBinding

/* TODO
 * moving hard-coded data into a stored local file?
 * adding/removing beacons from list
 * editing beacons (app-side; no plans to write to beacon)
 *  * coordinates
 *  * calibration (1m) RSSI value
 *  * display name
 *  * type (buzzer/start/destination)
 */
class CalibrationActivity : AppCompatActivity() {

    private lateinit var binding: ActivityCalibrationBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCalibrationBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupRecyclerView()
    }

    @UiThread
    private fun setupRecyclerView() {
        val view = binding.beaconsList

        view.adapter = BeaconsAdapter()

        view.layoutManager = LinearLayoutManager(this, RecyclerView.VERTICAL, false)
        view.isNestedScrollingEnabled = false
        view.itemAnimator.let {
            if (it is SimpleItemAnimator) {
                it.supportsChangeAnimations = false
            }
        }

        binding.beaconsList.isNestedScrollingEnabled = false
    }
}