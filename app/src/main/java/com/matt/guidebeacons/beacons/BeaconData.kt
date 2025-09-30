package com.matt.guidebeacons.beacons

class BeaconData {
    private val beaconProjects = mapOf(
        "80:EC:CC:CD:33:28" to Beacon("Losing Things", -60, 0.0, 1.0),
        "80:EC:CC:CD:33:7C" to Beacon("Happy Mornings", -57, 1.0, 2.0),
        "80:EC:CC:CD:33:7E" to Beacon("STEM", -59, 2.0, 2.0),
        "80:EC:CC:CD:33:58" to Beacon("Visual Clutter", -60, 2.0, 1.0),
        "00:3C:84:28:87:01" to Beacon("MAP", -58, 1.0, 0.0),
        "00:3C:84:28:77:AB" to Beacon("Dance", -60, 1.00, 1.0),
        "D8:F2:C8:9B:33:34" to Beacon("Origin", -62, 0.0, 0.0),
        "6C:B2:FD:34:CE:9E" to Beacon("Bee", -75, 0.5, 0.5)
    )

    fun getBeaconProjects(): Map<String, Beacon> {
        return beaconProjects
    }


    companion object {
        @Volatile
        private var instance: BeaconData? = null

        private fun getInstance(): BeaconData {
            return instance ?: synchronized(this) {
                instance ?: BeaconData().also { instance = it }
            }
        }

        fun getBeaconProjects(): Map<String, Beacon> {
            return getInstance().getBeaconProjects()
        }
    }
}