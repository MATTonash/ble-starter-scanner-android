package com.matt.guidebeacons.beacons

import android.content.Context
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@OptIn(kotlinx.serialization.InternalSerializationApi::class)
@Serializable
class RssiCollection private constructor(
    /**
     * MAC address for identification.
     */
    private val macAddress: String,
    /**
     * Display name for human-readability only. Usually the beacon name.
     */
    private val readableName: String
) {

    private val measurements = mutableListOf<RssiValue>()

    fun getMeasurements() : MutableList<RssiValue> {
        return measurements
    }

    /**
     * @return The JSON string that was written to file.
     */
    fun writeToFile(context: Context, prettyPrint: Boolean = false) : String {
        val fileName = getFileName(macAddress)
        timber.log.Timber.i("Saving RSSI collection for ${macAddress} (${readableName}) to ${context.filesDir.path}/${fileName}")
        val writer = Json { this.prettyPrint = prettyPrint }
        val json = writer.encodeToString(serializer(), this)
        context.openFileOutput(fileName, Context.MODE_PRIVATE).use {
            it.write(json.toByteArray())
        }

        return json
    }

    companion object {
        private fun getFileName(macAddress: String) : String {
            return "rssi_${macAddress.replace(':', '-')}.json"
        }

        /**
         * Attempts to read existing collected RSSI values from file using the given MAC address.
         * If there is no existing file, creates a new [RssiCollection] instead.
         * @param[macAddress] the MAC address for the beacon the collection of RSSI values belong to.
         * @param[readableName] see [RssiCollection.readableName].
         */
        fun readFromFile(context: Context, macAddress: String, readableName: String) : RssiCollection {
            val fileName = getFileName(macAddress)

            val file = java.io.File(context.filesDir, fileName)
            if (file.exists()) {
                timber.log.Timber.i("Reading ${context.filesDir.path}/${fileName}")
                context.openFileInput(fileName).bufferedReader().use {
                    val reader = Json { ignoreUnknownKeys = true }
                    return reader.decodeFromString(it.readText())
                }
            }
            else {
                timber.log.Timber.i("${context.filesDir.path}/${fileName} does not exist, creating memory instance")
                return RssiCollection(macAddress, readableName)
            }
        }
    }
}