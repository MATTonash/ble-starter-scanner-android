package com.matt.guidebeacons.activities

import android.content.Intent
import android.os.Bundle
import androidx.annotation.UiThread
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.SimpleItemAnimator
import com.matt.guidebeacons.beacons.Beacon
import com.matt.guidebeacons.beacons.BeaconData
import com.matt.guidebeacons.beacons.BeaconsAdapter
import com.matt.guidebeacons.constants.*
import com.punchthrough.blestarterappandroid.databinding.ActivityCalibrationBinding

/* TODO
 * move hard-coded data into a stored local file?
 * adding/removing beacons from list
 * editing beacons (app-side; no plans to write to beacon)
 *  * coordinates
 *  * calibration (1m) RSSI value
 *  * display name
 *  * type (buzzer/start/destination)
 *     * buzzer sensitivity
 *  * also store z-coordinate (maybe introduce coordinate data structure?)
 */
class CalibrationActivity : AppCompatActivity() {

    private lateinit var binding: ActivityCalibrationBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCalibrationBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupRecyclerView()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode != RESULT_OK) return

        binding.beaconsList.adapter!!.notifyDataSetChanged()
        BeaconData.writeBeaconsToFile(this, FILE_NAME_BEACONS)
    }

    @UiThread
    private fun setupRecyclerView() {
        val view = binding.beaconsList

        view.adapter = BeaconsAdapter { beacon: Beacon ->
            val editIntent = Intent(this, EditBeaconActivity::class.java)
            val selectedBeaconMacAddress = BeaconData.getBeaconMacAddress(beacon) // TODO: handle if null; show warning toast and don't start activity
            editIntent.putExtra(INTENT_EXTRA_SELECTED_BEACON_MAC, selectedBeaconMacAddress)
            startActivityForResult(editIntent, 0)
        }

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