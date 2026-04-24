
package com.matt.guidebeacons.mvvm

import androidx.room.RoomDatabase
import androidx.room.Database
import androidx.room.Room
import androidx.sqlite.db.SupportSQLiteDatabase
import com.matt.guidebeacons.mvvm.dao.BeaconDao
import com.matt.guidebeacons.mvvm.models.Beacon
import com.matt.guidebeacons.beacons.BeaconType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(entities = [Beacon::class], version = 1, exportSchema = false)
abstract class BeaconDatabase : RoomDatabase() {
    abstract fun beaconDao(): BeaconDao

    companion object {
        @Volatile
        private var INSTANCE: BeaconDatabase? = null

        fun getDatabase(context: android.content.Context): BeaconDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    BeaconDatabase::class.java,
                    "beacon_database"
                ).addCallback(object : RoomDatabase.Callback() {
                    override fun onCreate(db: SupportSQLiteDatabase) {
                        super.onCreate(db)

                        INSTANCE?.let { database ->
                            CoroutineScope(Dispatchers.IO).launch {
                                val dao = database.beaconDao()

                                dao.insertBeacon(Beacon("Losing Things", -60, "80:EC:CC:CD:33:28", BeaconType.DEFAULT, 0.0, 0.0, 1.0, 0.0))
                                dao.insertBeacon(Beacon("Happy Mornings", -57, "80:EC:CC:CD:33:7C", BeaconType.DEFAULT, 0.0, 1.0, 2.0, 0.0))
                                dao.insertBeacon(Beacon("STEM", -59, "80:EC:CC:CD:33:7E", BeaconType.DEFAULT, 0.0, 2.0, 2.0, 0.0))
                                dao.insertBeacon(Beacon("Visual Clutter", -60, "80:EC:CC:CD:33:58", BeaconType.DEFAULT, 0.0, 2.0, 1.0, 0.0))
                                dao.insertBeacon(Beacon("MAP", -58, "00:3C:84:28:87:01", BeaconType.DEFAULT, 0.0, 1.0, 0.0, 0.0))
                                dao.insertBeacon(Beacon("Dance", -60, "00:3C:84:28:77:AB", BeaconType.DEFAULT, 0.0, 1.0, 1.0, 0.0))
                                dao.insertBeacon(Beacon("Origin", -62, "D8:F2:C8:9B:33:34", BeaconType.DEFAULT, 0.0, 0.0, 0.0, 0.0))
                                dao.insertBeacon(Beacon("Bee", -75, "6C:B2:FD:34:CE:9E", BeaconType.DEFAULT, 0.0, 0.5, 0.5, 0.0))
                            }
                        }
                    }
                }).build()

                INSTANCE = instance
                instance
            }
        }
    }
}
