package com.matt.guidebeacons.mvvm.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow
import com.matt.guidebeacons.mvvm.models.Beacon

@Dao
interface BeaconDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBeacon(beacon: Beacon)

    @Query("SELECT * FROM beacon")
    suspend fun getAllBeacons(): List<Beacon>

    // Reactive stream of all beacons
    @Query("SELECT * FROM beacon")
    fun getAllBeaconsFlow(): Flow<List<Beacon>>

    @Query("SELECT * FROM beacon WHERE name = :name LIMIT 1")
    suspend fun getBeaconByName(name: String): Beacon?

    @Query("DELETE FROM beacon WHERE name = :name")
    suspend fun deleteByName(name: String): Int
}
