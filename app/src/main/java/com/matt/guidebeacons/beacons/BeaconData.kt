package com.matt.guidebeacons.beacons

import android.content.Context
import com.punchthrough.blestarterappandroid.R
import kotlinx.serialization.builtins.MapSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json
import java.io.File
import java.io.InputStream

class BeaconData {
    private val beaconProjects = mutableMapOf(
        "80:EC:CC:CD:33:28" to Beacon("Losing Things", -60, 0.0, 1.0),
        "80:EC:CC:CD:33:7C" to Beacon("Happy Mornings", -57, 1.0, 2.0),
        "80:EC:CC:CD:33:7E" to Beacon("STEM", -59, 2.0, 2.0),
        "80:EC:CC:CD:33:58" to Beacon("Visual Clutter", -60, 2.0, 1.0),
        "00:3C:84:28:87:01" to Beacon("MAP", -58, 1.0, 0.0),
        "00:3C:84:28:77:AB" to Beacon("Dance", -60, 1.00, 1.0),
        "D8:F2:C8:9B:33:34" to Beacon("Origin", -62, 0.0, 0.0),
        "6C:B2:FD:34:CE:9E" to Beacon("Bee", -75, 0.5, 0.5)
    )

    fun getBeaconProjects(): MutableMap<String, Beacon> {
        return beaconProjects
    }

    fun setBeaconProjects(beacons: MutableMap<String, Beacon>) {
        // clear and replace rather than setting to new reference to prevent breaking
        // existing usages of `val beaconProjects = BeaconData.getBeaconProjects()`
        // todo: consider checking/refactoring existing usages?
        beaconProjects.clear()
        beaconProjects.putAll(beacons)
    }

    fun getBeaconMacAddress(beacon: Beacon): String? {
        val filtered = beaconProjects.filterValues { it == beacon }

        return if (filtered.keys.isEmpty()) null else filtered.keys.first()
    }


    companion object {
        @Volatile
        private var instance: BeaconData? = null

        private fun getInstance(): BeaconData {
            return instance ?: synchronized(this) {
                instance ?: BeaconData().also { instance = it }
            }
        }

        fun getBeaconProjects(): MutableMap<String, Beacon> {
            return getInstance().getBeaconProjects()
        }

        fun getBeaconMacAddress(beacon: Beacon): String? {
            return getInstance().getBeaconMacAddress(beacon)
        }

        // https://developer.android.com/training/data-storage/app-specific#kotlin
        // https://developer.android.com/training/data-storage/shared/documents-files
        fun writeBeaconsToFile(context: Context, fileName: String) {
            timber.log.Timber.i("Saving beacons to ${context.filesDir.path}/${fileName}")
            val json = Json.encodeToString(MapSerializer(String.serializer(), Beacon.serializer()), getBeaconProjects())
            context.openFileOutput(fileName, Context.MODE_PRIVATE).use {
                 it.write(json.toByteArray())
            }
        }

        fun readBeaconsFromFile(context: Context, fileName: String) {
            readBeaconsFromInputStream(context.openFileInput(fileName))
        }

        private fun readBeaconsFromInputStream(stream: InputStream) {
            stream.bufferedReader().use {
                val beacons = Json.decodeFromString<MutableMap<String, Beacon>>(it.readText())
                getInstance().setBeaconProjects(beacons)
                timber.log.Timber.i("Loaded ${beacons.size} beacon(s).")
            }
        }

        fun initialiseBeaconData(context: Context, fileName: String) {
            // Test serialization  (saves to /data/data/com.punchthrough.blestarterappandroid/files/default_beacons.json); use this to update /res/raw/default_beacons.json ?
            writeBeaconsToFile(context, "default_beacons.json")

            if (File(context.filesDir.path, fileName).exists()) {
                timber.log.Timber.i("Loading beacons from ${context.filesDir.path}/${fileName}")
                readBeaconsFromFile(context, fileName)
            }
            else {
                timber.log.Timber.i("Could not find ${context.filesDir.path}/${fileName}, loading default_beacons.json")
                readBeaconsFromInputStream(context.resources.openRawResource(R.raw.default_beacons))
            }
        }
    }
}