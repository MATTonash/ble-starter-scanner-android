package com.matt.guidebeacons.beacons

import android.content.Context
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@OptIn(kotlinx.serialization.InternalSerializationApi::class)
@Serializable
class RssiCollection(
    /**
     * MAC address for identification.
     */
    private val macAddress: String,
    /**
     * Display name for readability only.
     */
    private val beaconName: String
) {

    private val measurements = mutableListOf<RssiValue>()


    fun writeToFile(context: Context, prettyPrint: Boolean = false) {
        val fileName = getFileName(macAddress)
        timber.log.Timber.i("Saving RSSI collection for ${macAddress} (${beaconName}) to ${context.filesDir.path}/${fileName}")
        val writer = Json { this.prettyPrint = prettyPrint }
        val json = writer.encodeToString(serializer(), this)
        context.openFileOutput(fileName, Context.MODE_PRIVATE).use {
            it.write(json.toByteArray())
        }
    }

    companion object {
        private fun getFileName(macAddress: String) : String {
            return "rssi_${macAddress.replace(':', '-')}.json"
        }

        fun readFromFile(context: Context, macAddress: String) : RssiCollection {
            val fileName = getFileName(macAddress)
            context.openFileInput(fileName).bufferedReader().use {
                val reader = Json { ignoreUnknownKeys = true }
                return reader.decodeFromString(it.readText())
            }
        }
    }
}