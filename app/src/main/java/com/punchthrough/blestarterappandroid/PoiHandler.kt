package com.punchthrough.blestarterappandroid

import com.punchthrough.blestarterappandroid.isPointInPolygon
import com.punchthrough.blestarterappandroid.distance

// importing the neccessary functions from the geometry utils file


import android.hardware.camera2.params.MeteringRectangle

enum class POIType {
    NONE,
    START,
    END,
    PATH,

}
const val TOLERANCE = 0.5f // tolerance for path proximity
object PoiHandler {
   // creating this function to check the user if they are at the POI
    fun checkUserifPOI(
        user: ConfigPoint?,
        startRectangle: List<List<ConfigPoint>>,
        endRectangles: List<List<ConfigPoint>>,
        paths: List<List<ConfigPoint>>,
        threshold: Float = TOLERANCE

    ): POIType{
        val user = user ?: return POIType.NONE

       startRectangle.forEach { rect ->
           if (isPointInPolygon(user, rect)) return POIType.START
       }
       endRectangles.forEach { rect ->
           if (isPointInPolygon(user, rect)) return POIType.END
       }
       paths.flatten().forEach { pathPoint ->
           if (distance(user.x, user.y, pathPoint.x, pathPoint.y) < threshold) return POIType.PATH
       }
       return POIType.NONE

    }

    fun handleUserAtPOI(
        user: ConfigPoint?,
        startRectangle: List<List<ConfigPoint>>,
        endRectangles: List<List<ConfigPoint>>,
        paths: List<List<ConfigPoint>>,
        threshold: Float = 0.5f,
        onStart: (() -> Unit)? = null,
        onEnd: (() -> Unit)? = null,
        onPath: (() -> Unit)? = null,
        onNone: (() -> Unit)? = null
    ) {
        val user = user ?: return onNone?.invoke() ?: Unit

        startRectangle.forEach { rect ->
            if (isPointInPolygon(user, rect)) {
                onStart?.invoke()
                return
            }
        }
        endRectangles.forEach { rect ->
            if (isPointInPolygon(user, rect)) {
                onEnd?.invoke()
                return
            }
        }
        paths.flatten().forEach { pathPoint ->
            if (distance(user.x, user.y, pathPoint.x, pathPoint.y) < threshold) {
                onPath?.invoke()
                return
            }
        }
        onNone?.invoke()
    }



}
