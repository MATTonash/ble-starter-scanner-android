/*
 * Copyright 2026 Punch Through Design LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.matt.guidebeacons.mvvm

import androidx.room.RoomDatabase
import androidx.room.Database
import androidx.room.Room
import com.matt.guidebeacons.mvvm.dao.BeaconDao
import com.matt.guidebeacons.mvvm.models.Beacon


@Database(entities = [Beacon::class], version = 1, exportSchema = false)

abstract class BeaconDatabase : RoomDatabase() {
    abstract fun beaconDao(): BeaconDao

    companion object {
        @Volatile
        private var INSTANCE: BeaconDatabase? = null

        fun getDatabase(context: android.content.Context): BeaconDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder<BeaconDatabase>(
                    context.applicationContext,
                    BeaconDatabase::class.java,
                    "beacon_database"
                ).build()
                INSTANCE = instance
                instance
            }

        }
    }
}

