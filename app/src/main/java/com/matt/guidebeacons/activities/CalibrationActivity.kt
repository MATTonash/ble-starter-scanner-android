package com.matt.guidebeacons.activities

import android.content.Intent
import android.os.Bundle
import androidx.annotation.UiThread
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.SimpleItemAnimator
import com.google.android.material.snackbar.Snackbar
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
 *     * buzzer sensitivity (rssi strength proximity? e.g. if rssi is greater than sensitivity, buzz)
 *  * also store z-coordinate (maybe introduce coordinate data structure?)
 */

private const val REQUEST_EDIT = 1
private const val REQUEST_ADD = 2

class CalibrationActivity : AppCompatActivity() {

    private lateinit var binding: ActivityCalibrationBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCalibrationBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupRecyclerView()
        binding.addBeacon.setOnClickListener {
            val addIntent = Intent(this, InputMacAddressActivity::class.java)
            startActivityForResult(addIntent, REQUEST_ADD)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        when (requestCode) {
            REQUEST_EDIT -> onEditActivityResult(resultCode, data)
            REQUEST_ADD -> onAddActivityResult(resultCode, data)
            else -> timber.log.Timber.w("Received invalid result code \"${resultCode}\"")
        }
    }

    private fun onEditActivityResult(resultCode: Int, data: Intent?) {
        if (resultCode != RESULT_OK) return

        binding.beaconsList.adapter!!.notifyDataSetChanged()
        BeaconData.writeBeaconsToFile(this, FILE_NAME_BEACONS)

        val beaconName = data?.getStringExtra(INTENT_EXTRA_DELETED_BEACON)
        if (!beaconName.isNullOrBlank()) {
            Snackbar.make(binding.root, "Deleted beacon \"${beaconName}\"", Snackbar.LENGTH_LONG).show()
        }
    }

    private fun onAddActivityResult(resultCode: Int, data: Intent?) {
        if (resultCode != RESULT_OK) return

        val editIntent = Intent(this, EditBeaconActivity::class.java)
        val macAddress = data!!.getStringExtra(INTENT_EXTRA_SELECTED_BEACON_MAC)
        editIntent.putExtra(INTENT_EXTRA_SELECTED_BEACON_MAC, macAddress)

        if (!BeaconData.getBeaconProjects().containsKey(macAddress)) {
            BeaconData.getBeaconProjects().put(macAddress!!, Beacon("New beacon", 0, 0.0, 0.0))
        }
        else {
            editIntent.putExtra(INTENT_EXTRA_ADDED_EXISTING_MAC, true)
        }

        startActivityForResult(editIntent, REQUEST_EDIT)
    }

    @UiThread
    private fun setupRecyclerView() {
        val view = binding.beaconsList

        view.adapter = BeaconsAdapter { beacon: Beacon ->
            val editIntent = Intent(this, EditBeaconActivity::class.java)
            val selectedBeaconMacAddress = BeaconData.getBeaconMacAddress(beacon) // TODO: handle if null; show warning toast and don't start activity
            editIntent.putExtra(INTENT_EXTRA_SELECTED_BEACON_MAC, selectedBeaconMacAddress)
            startActivityForResult(editIntent, REQUEST_EDIT)
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