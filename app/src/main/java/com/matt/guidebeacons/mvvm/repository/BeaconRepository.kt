package com.matt.guidebeacons.mvvm.repository

import android.content.Context
import kotlinx.coroutines.flow.Flow
import com.matt.guidebeacons.mvvm.BeaconDatabase
import com.matt.guidebeacons.mvvm.models.Beacon

class BeaconRepository(context: Context) {

    private val beaconDao = BeaconDatabase.getDatabase(context).beaconDao()

    // Expose reactive stream from Room
    fun getAllBeaconsStream(): Flow<List<Beacon>> = beaconDao.getAllBeaconsFlow()

    suspend fun insertBeacon(beacon: Beacon) {
        beaconDao.insertBeacon(beacon)
    }

    suspend fun getAllBeacons(): List<Beacon> = beaconDao.getAllBeacons()
}
