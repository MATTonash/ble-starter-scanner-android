
package com.punchthrough.blestarterappandroid

import kotlin.math.sqrt

// this will be used to check if the user is within a given polygon
fun isPointInPolygon(point: ConfigPoint, polygon: List<ConfigPoint>): Boolean {
    var result = false
    var j = polygon.size - 1
    for (i in polygon.indices) {
        if ((polygon[i].y > point.y) != (polygon[j].y > point.y) &&
            (point.x < (polygon[j].x - polygon[i].x) * (point.y - polygon[i].y) / (polygon[j].y - polygon[i].y) + polygon[i].x)
        ) {
            result = !result
        }
        j = i
    }
    return result
}

//this calculates the distance between two points
fun distance(x1: Float, y1: Float, x2: Float, y2: Float): Float {
    return sqrt((x2 - x1) * (x2 - x1) + (y2 - y1) * (y2 - y1))
}