package com.matt.guidebeacons.beacons

import android.app.Application
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import com.matt.guidebeacons.mvvm.BeaconDatabase
import com.matt.guidebeacons.mvvm.repository.BeaconRepository
import com.matt.guidebeacons.beacons.BeaconData
import com.matt.guidebeacons.beacons.Beacon as DomainBeacon
import com.matt.guidebeacons.mvvm.models.Beacon as DbBeacon

class GuideBeaconsApplication : Application() {
    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()

        val repo = BeaconRepository(this)

        // Populate your in-memory BeaconData from Room (map DbBeacon -> DomainBeacon)
        appScope.launch {
            val dbList = repo.getAllBeacons()
            val map = mutableMapOf<String, DomainBeacon>()
            dbList.forEach { db ->
                // Adjust mapping to match your DomainBeacon constructor and fields
                val mac = (db.macAddress ?: "").uppercase()
                val domain = DomainBeacon(
                    db.name,
                    db.RSSI,
                    db.x_coordinate ?: 0.0,
                    db.y_coordinate ?: 0.0,
                    db.z_coordinate ?: 0.0
                )
                map[mac] = domain
            }



            BeaconData.setBeaconProjects(map)
        }
    }
}

object BeaconData {
    private val projects = mutableMapOf<String, DomainBeacon>()

    fun setBeaconProjects(map: Map<String, DomainBeacon>) {
        projects.clear()
        projects.putAll(map)
    }

    fun getBeacon(mac: String): DomainBeacon? = projects[mac.uppercase()]
}