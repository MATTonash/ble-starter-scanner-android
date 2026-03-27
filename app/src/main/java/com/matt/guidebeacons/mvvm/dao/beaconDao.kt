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

package com.matt.guidebeacons.mvvm.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.matt.guidebeacons.mvvm.models.Beacon

@Dao
interface BeaconDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBeacon(beacon: Beacon)

    @Query("SELECT * FROM beacon")
    suspend fun getAllBeacons(): List<Beacon>

    @Query("SELECT * FROM beacon WHERE name = :name LIMIT 1")
    suspend fun getBeaconByName(name: String): Beacon?


    @Query ("DELETE FROM beacon WHERE name = :name")
    suspend fun deleteByName(name: String): Int
}