package com.matt.guidebeacons.maps

import android.content.Context
import com.punchthrough.blestarterappandroid.R

class MapData(context: Context) {

    private val maps = mutableMapOf(
        "matt" to Map("matt", "matt_map_beacons.json", 30, R.drawable.map, context, 9f, 16f)
    )

    fun getMaps(): MutableMap<String, Map> {
        return maps
    }


    companion object {
        @Volatile
        private var instance: MapData? = null

        private fun getInstance(): MapData? {
            return instance ?: synchronized(this) {
                instance
            }
        }

        fun initContext(context: Context) {
            instance ?: synchronized(this) {
                instance ?: MapData(context).also { instance = it }
            }
        }

        fun getMap(name: String): Map? {
            return instance?.getMaps()?.get(name)
        }
    }
}