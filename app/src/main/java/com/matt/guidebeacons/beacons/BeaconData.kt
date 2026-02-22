package com.matt.guidebeacons.beacons

import android.content.Context
import com.matt.guidebeacons.beacons.BeaconData.Companion.getBeaconProjects
import com.matt.guidebeacons.utils.UppercaseSerializer
import com.punchthrough.blestarterappandroid.R
import kotlinx.serialization.KSerializer
import kotlinx.serialization.builtins.MapSerializer
import kotlinx.serialization.json.Json
import java.io.File
import java.io.InputStream

/**
 * Singleton that contains the collection of [Beacon]s used throughout the app.
 */
class BeaconData {
    /**
     * This hard-coded map of beacon projects is only used by the app
     * to generate a test json file on launch and is then overwritten.
     * To add new built-in beacons, edit `/res/raw/default_beacons.json` instead.
     * @see[getBeaconProjects]
     */
    private val beaconProjects = mutableMapOf(
        "80:EC:CC:CD:33:28" to Beacon("Losing Things", -60, 0.0, 0.0, 0.0),
        "80:EC:CC:CD:33:7C" to Beacon("Happy Mornings", -57, 0.0, 1.0, 0.0),
//        "80:EC:CC:CD:33:7E" to Beacon("STEM", -59, 2.0, 2.0, 0.0),
//        "80:EC:CC:CD:33:58" to Beacon("Visual Clutter", -60, 2.0, 1.0, 0.0),
//        "00:3C:84:28:87:01" to Beacon("MAP", -58, 1.0, 0.0, 0.0),
//        "00:3C:84:28:77:AB" to Beacon("Dance", -60, 1.00, 1.0, 0.0),
        "D8:F2:C8:9B:33:34" to Beacon("Origin", -62, 0.0, 2.0, 0.0),
        "6C:B2:FD:34:CE:9E" to Beacon("Bee", -75, 1.0, 0.0, 0.0),
        "6C:B2:FD:34:B8:C4" to Beacon("05", -75, 1.0, 1.0, 0.0),
        "F4:65:0B:40:77:86" to Beacon("ESP32_0", -60, 1.0, 2.0, 0.0),
        "08:A6:F7:B0:71:CA" to Beacon("ESP32_1", -60, 3.0, 0.0, 0.0),
        "2C:BC:BB:4D:7D:92" to Beacon("ESP32_2", -60, 2.0, 1.0, 0.0),
        "08:A6:F7:B0:79:0A" to Beacon("ESP32_3", -60, 2.0, 2.0, 0.0),
        "78:42:1C:66:0B:B6" to Beacon("buzzer 1", -60, 1.5, 2.0, 0.0)
    )

    /**
     * Map of MAC addresses to [Beacon] instances.
     * Since MAC address strings are being used as keys, they are case-sensitive.
     */
    fun getBeaconProjects(): MutableMap<String, Beacon> {
        return beaconProjects
    }

    /**
     * Technically clears existing beacons and copies the passed in beacons into the existing map,
     * to avoid replacing the map reference. This is done to prevent breaking
     * existing usages of `val beaconProjects = BeaconData.getBeaconProjects()`
     */
    fun setBeaconProjects(beacons: Map<String, Beacon>) {
        beaconProjects.clear()
        beaconProjects.putAll(beacons)
    }

    /**
     * Filters [beaconProjects] to find the MAC address for the given beacon.
     * @return a MAC address string if the beacon is found, null otherwise.
     */
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

        /**
         * @see[BeaconData.getBeaconProjects]
         */
        fun getBeaconProjects(): MutableMap<String, Beacon> {
            return getInstance().getBeaconProjects()
        }

        /**
         * @see[BeaconData.getBeaconMacAddress]
         */
        fun getBeaconMacAddress(beacon: Beacon): String? {
            return getInstance().getBeaconMacAddress(beacon)
        }

        // https://developer.android.com/training/data-storage/app-specific#kotlin
        // https://developer.android.com/training/data-storage/shared/documents-files
        fun writeBeaconsToFile(context: Context, fileName: String, prettyPrint: Boolean = false) {
            timber.log.Timber.i("Saving beacons to ${context.filesDir.path}/${fileName}")
            val writer = Json { this.prettyPrint = prettyPrint }
            val json = writer.encodeToString(getSerializer(), getBeaconProjects())
            context.openFileOutput(fileName, Context.MODE_PRIVATE).use {
                 it.write(json.toByteArray())
            }
        }

        fun readBeaconsFromFile(context: Context, fileName: String) {
            readBeaconsFromInputStream(context.openFileInput(fileName))
        }

        private fun readBeaconsFromInputStream(stream: InputStream) {
            stream.bufferedReader().use {
                val reader = Json { ignoreUnknownKeys = true }
                val beacons = reader.decodeFromString(getSerializer(), it.readText())
                getInstance().setBeaconProjects(beacons)
                timber.log.Timber.i("Loaded ${beacons.size} beacon(s).")
            }
        }

        private fun getSerializer(): KSerializer<Map<String, Beacon>> {
            return MapSerializer(UppercaseSerializer, Beacon.serializer())
        }

        fun initialiseBeaconData(context: Context, fileName: String) {
            // Test serialization (saves to /data/data/com.punchthrough.blestarterappandroid/files/default_beacons.json); use this to update /res/raw/default_beacons.json ?
            writeBeaconsToFile(context, "default_beacons.json", true)

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