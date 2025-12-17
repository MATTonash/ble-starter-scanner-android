/*
 * Copyright 2025 Punch Through Design LLC
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

package com.punchthrough.blestarterappandroid

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.util.Xml
import android.view.View
import androidx.annotation.RawRes
import org.xmlpull.v1.XmlPullParser
import kotlin.math.hypot
import kotlin.math.min
import kotlin.math.sqrt
import kotlin.math.abs
import android.speech.tts.TextToSpeech
import android.view.MotionEvent
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator

private const val LINE_WIDTH = 20f
private const val DEFAULT_MAX_X = 5f
private const val DEFAULT_MAX_Y = 5f
enum class POIState { START, END, PATH, NONE }

data class ConfigPoint(val x: Float, val y: Float)

data class UserMapConfig(
    val beacons: List<ConfigPoint> = emptyList(),
    val polygons: List<List<ConfigPoint>> = emptyList(),
    val paths: List<List<ConfigPoint>> = emptyList(),
    val startRectangles: List<List<ConfigPoint>> = emptyList(),
    val endRectangles: List<List<ConfigPoint>> = emptyList(),
    val maxX: Float = DEFAULT_MAX_X,
    val maxY: Float = DEFAULT_MAX_Y
)



class UserMapView(context: Context, attrs: AttributeSet? = null) : View(context, attrs), TextToSpeech.OnInitListener {

    // Logical coordinate system
    private var maxX = DEFAULT_MAX_X
    private var maxY = DEFAULT_MAX_Y

    // Screen mapping
    private var scale = 1f
    private var offsetX = 0f
    private var offsetY = 0f

    // Paints
    private val screenBackgroundPaint = Paint().apply { color = Color.DKGRAY; style = Paint.Style.FILL }
    private val mapBackgroundPaint = Paint().apply { color = Color.LTGRAY; style = Paint.Style.FILL }
    private val beaconPaint = Paint().apply { color = Color.CYAN; style = Paint.Style.FILL }
    private val polygonPaint = Paint().apply { color = Color.BLACK; style = Paint.Style.FILL }
    private val linePaint = Paint().apply { color = Color.BLUE; style = Paint.Style.STROKE; strokeWidth = LINE_WIDTH }
    private val startRectPaint = Paint().apply { color = Color.YELLOW; style = Paint.Style.FILL }
    private val endRectPaint = Paint().apply { color = Color.MAGENTA; style = Paint.Style.FILL }
    private val userPaint = Paint().apply { color = Color.RED; style = Paint.Style.FILL }
    private val userAnglePaint = Paint().apply { color = Color.GREEN; style = Paint.Style.FILL }

    // Geometry
    private var userPosition: ConfigPoint? = null
    private var userAngle: Float? = null
    private val beacons = mutableListOf<ConfigPoint>()
    private val polygons = mutableListOf<List<ConfigPoint>>()
    private val paths = mutableListOf<List<ConfigPoint>>()
    private val userDrawnPath = mutableListOf<ConfigPoint>()
    private val startRectangles = mutableListOf<List<ConfigPoint>>()
    private val endRectangles = mutableListOf<List<ConfigPoint>>()

    private var lastPoiState: POIState = POIState.NONE

    // variable for the text to speech
    private var tts: TextToSpeech? = null

    init{
        tts = TextToSpeech(context, this)
    }


    override fun onDetachedFromWindow() {
      tts?.shutdown()
        super.onDetachedFromWindow()
    }


    override fun onInit(status: Int){}

    private fun speak(text:String){
        tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, null)
    }

    // Vibrate briefly and speak a warning
    private fun triggerWarning() {
        // Vibrate
        val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        vibrator?.let {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                it.vibrate(VibrationEffect.createOneShot(300, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION")
                it.vibrate(300)
            }
        }
        // Speak
        speak("Warning")
    }

    /**
     * Loads XML configuration from res/raw
     */
    fun loadConfigFromRawXml(@RawRes resId: Int) {
        context.resources.openRawResource(resId).use { inputStream ->
            val parser = Xml.newPullParser()
            parser.setInput(inputStream, null)
            val config = parseXmlConfigFromParser(parser)
            applyConfig(config)
        }
    }

    /**
     * Updates self based to given config values
     */
    fun applyConfig(config: UserMapConfig) {
        beacons.clear()
        beacons.addAll(config.beacons)

        polygons.clear()
        polygons.addAll(config.polygons)

        paths.clear()
        paths.addAll(config.paths)

        startRectangles.clear()
        startRectangles.addAll(config.startRectangles)

        endRectangles.clear()
        endRectangles.addAll(config.endRectangles)

        maxX = config.maxX
        maxY = config.maxY

        setUserPosition((startRectangles[0][0].x + startRectangles[0][2].x) / 2, (startRectangles[0][0].y + startRectangles[0][2].y) / 2)
    }

    /**
     * Returns userMapConfig object with values specified as from the given parser object
     */
    private fun parseXmlConfigFromParser(parser: XmlPullParser): UserMapConfig {
        val beacons = mutableListOf<ConfigPoint>()
        val polygons = mutableListOf<List<ConfigPoint>>()
        val paths = mutableListOf<List<ConfigPoint>>()
        val startRects = mutableListOf<List<ConfigPoint>>()
        val endRects = mutableListOf<List<ConfigPoint>>()

        var currentPolygon: MutableList<ConfigPoint>? = null
        var currentPath: MutableList<ConfigPoint>? = null
        var maxX = DEFAULT_MAX_X
        var maxY = DEFAULT_MAX_Y

        var eventType = parser.eventType
        while (eventType != XmlPullParser.END_DOCUMENT) {
            when (eventType) {
                XmlPullParser.START_TAG -> {
                    when (parser.name) {
                        "coordinateSystem" -> {
                            maxX = parser.getAttributeValue(null, "maxX")?.toFloat() ?: DEFAULT_MAX_X
                            maxY = parser.getAttributeValue(null, "maxY")?.toFloat() ?: DEFAULT_MAX_Y
                        }
                        "beacon" -> {
                            val x = parser.getAttributeValue(null, "x").toFloat()
                            val y = parser.getAttributeValue(null, "y").toFloat()
                            beacons.add(ConfigPoint(x, y))
                        }
                        "polygon" -> currentPolygon = mutableListOf()
                        "path" -> currentPath = mutableListOf()
                        "point" -> {
                            val x = parser.getAttributeValue(null, "x").toFloat()
                            val y = parser.getAttributeValue(null, "y").toFloat()
                            when {
                                currentPolygon != null -> currentPolygon.add(ConfigPoint(x, y))
                                currentPath != null -> currentPath.add(ConfigPoint(x, y))
                            }
                        }
                        "rectangle" -> {
                            val x = parser.getAttributeValue(null, "x").toFloat()
                            val y = parser.getAttributeValue(null, "y").toFloat()
                            val width = parser.getAttributeValue(null, "width").toFloat()
                            val height = parser.getAttributeValue(null, "height").toFloat()
                            val rectPoints = listOf(
                                ConfigPoint(x, y),
                                ConfigPoint(x + width, y),
                                ConfigPoint(x + width, y + height),
                                ConfigPoint(x, y + height)
                            )
                            polygons.add(rectPoints)
                        }
                        "start" -> {
                            val x = parser.getAttributeValue(null, "x").toFloat()
                            val y = parser.getAttributeValue(null, "y").toFloat()
                            val width = parser.getAttributeValue(null, "width").toFloat()
                            val height = parser.getAttributeValue(null, "height").toFloat()
                            val rectPoints = listOf(
                                ConfigPoint(x, y),
                                ConfigPoint(x + width, y),
                                ConfigPoint(x + width, y + height),
                                ConfigPoint(x, y + height)
                            )
                            startRects.add(rectPoints)
                        }
                        "end" -> {
                            val x = parser.getAttributeValue(null, "x").toFloat()
                            val y = parser.getAttributeValue(null, "y").toFloat()
                            val width = parser.getAttributeValue(null, "width").toFloat()
                            val height = parser.getAttributeValue(null, "height").toFloat()
                            val rectPoints = listOf(
                                ConfigPoint(x, y),
                                ConfigPoint(x + width, y),
                                ConfigPoint(x + width, y + height),
                                ConfigPoint(x, y + height)
                            )
                            endRects.add(rectPoints)
                        }
                    }
                }
                XmlPullParser.END_TAG -> {
                    when (parser.name) {
                        "polygon" -> currentPolygon?.let { polygons.add(it.toList()); currentPolygon = null }
                        "path" -> currentPath?.let { paths.add(it.toList()); currentPath = null }
                    }
                }
            }
            eventType = parser.next()
        }

        return UserMapConfig(
            beacons = beacons,
            polygons = polygons,
            paths = paths,
            startRectangles = startRects,
            endRectangles = endRects,
            maxX = maxX,
            maxY = maxY
        )

    }

    /**
     * Updates the current userAngle (which way does the map show the user facing?)
     * and redraws the map
     */
    fun setUserAngle(angle: Float?) {
        userAngle = angle
        invalidate()
    }

    /**
     * Updates the current userPosition and redraws the map
     */

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (event.action == MotionEvent.ACTION_DOWN) {
            val mapX = ((event.x - offsetX) / scale).coerceIn(0f, maxX)
            val mapY = ((event.y - offsetY) / scale).coerceIn(0f, maxY)
            setUserPosition(mapX, mapY)
            performClick()
            return true
        }
        return super.onTouchEvent(event)
    }

    fun setUserPosition(x: Float, y: Float) {
        val clampedX = x.coerceIn(0f, maxX)
        val clampedY = y.coerceIn(0f, maxY)
        userPosition = ConfigPoint(clampedX, clampedY)
        invalidate()

        // Tolerance in map coordinates: matches the drawn user circle radius (0.25f * scale => 0.25f in map units)
        val tolerance = 0.25f

        // Determine POI state with tolerance checks
        val atStart = startRectangles.any { polygonContainsPoint(it, userPosition!!) }
        val atEnd = endRectangles.any { polygonContainsPoint(it, userPosition!!) }
        val onAnyPath = paths.any { pathIsNear(userPosition!!, it, tolerance) }

        val newState = when {
            atStart -> POIState.START
            atEnd -> POIState.END
            onAnyPath -> POIState.PATH
            else -> POIState.NONE
        }

        // Only act if state changed
        if (newState != lastPoiState) {
            when (newState) {
                POIState.START -> speak("Hello!, You are at Starting Position")
                POIState.END -> speak("You have Reached Your Destination")
                POIState.PATH -> { /* Optionally speak or handle being on path */ }
                POIState.NONE -> triggerWarning()
            }
            lastPoiState = newState
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val scaleX = width / maxX
        val scaleY = height / maxY
        scale = min(scaleX, scaleY)

        offsetX = (width - maxX * scale) / 2f
        offsetY = (height - maxY * scale) / 2f

        // Screen and map backgrounds
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), screenBackgroundPaint)
        canvas.drawRect(offsetX, offsetY, offsetX + maxX * scale, offsetY + maxY * scale, mapBackgroundPaint)

        polygons.forEach { drawPolygon(canvas, it, polygonPaint) }
        startRectangles.forEach { drawPolygon(canvas, it, startRectPaint) }
        endRectangles.forEach { drawPolygon(canvas, it, endRectPaint) }

        beacons.forEach { point ->
            val px = offsetX + point.x * scale
            val py = offsetY + point.y * scale
            canvas.drawCircle(px, py, 0.2f * scale, beaconPaint)
        }

        paths.forEach { points ->
            for (i in 0 until points.size - 1) {
                val start = points[i]
                val end = points[i + 1]
                canvas.drawLine(
                    offsetX + start.x * scale, offsetY + start.y * scale,
                    offsetX + end.x * scale, offsetY + end.y * scale,
                    linePaint
                )
            }
        }

        userPosition?.let { user ->
            val px = offsetX + user.x * scale
            val py = offsetY + user.y * scale
            val angleSize = 60f
            val relativeAngleLength = 0.8f
            userAngle?.let { angle ->
                canvas.drawArc(px - relativeAngleLength * scale, py - relativeAngleLength * scale, px + relativeAngleLength * scale, py + relativeAngleLength * scale, angle - angleSize / 2, angleSize, true, userAnglePaint)
            }
            val relativeCircleSize = 0.25f
            canvas.drawCircle(px, py, relativeCircleSize * scale, userPaint)
        }

    }

    /**
     * Draws a polygon to a canvas with a paint
     */
    private fun drawPolygon(canvas: Canvas, points: List<ConfigPoint>, paint: Paint) {
        val path = Path()
        points.forEachIndexed { index, point ->
            val px = offsetX + point.x * scale
            val py = offsetY + point.y * scale
            if (index == 0) path.moveTo(px, py) else path.lineTo(px, py)
        }
        path.close()
        canvas.drawPath(path, paint)
    }

    // Ray-casting algorithm for point-in-polygon
    private fun polygonContainsPoint(polygon: List<ConfigPoint>, point: ConfigPoint): Boolean {
        var inside = false
        val n = polygon.size
        var j = n - 1
        for (i in 0 until n) {
            val xi = polygon[i].x
            val yi = polygon[i].y
            val xj = polygon[j].x
            val yj = polygon[j].y
            val intersect = ((yi > point.y) != (yj > point.y)) &&
                    (point.x < (xj - xi) * (point.y - yi) / (yj - yi + 0.0f) + xi)
            if (intersect) inside = !inside
            j = i
        }
        return inside
    }

    // Check whether point is within 'tolerance' distance of any segment in the path
    private fun pathIsNear(point: ConfigPoint, path: List<ConfigPoint>, tolerance: Float): Boolean {
        if (path.size < 2) return false
        for (i in 0 until path.size - 1) {
            val a = path[i]
            val b = path[i + 1]
            if (distancePointToSegment(point.x, point.y, a.x, a.y, b.x, b.y) <= tolerance) {
                return true
            }
        }
        return false
    }

    // Distance from point (px,py) to segment (x1,y1)-(x2,y2)
    private fun distancePointToSegment(px: Float, py: Float, x1: Float, y1: Float, x2: Float, y2: Float): Float {
        val dx = x2 - x1
        val dy = y2 - y1
        if (dx == 0f && dy == 0f) {
            return distance(px, py, x1, y1)
        }
        val t = ((px - x1) * dx + (py - y1) * dy) / (dx * dx + dy * dy)
        val clampedT = t.coerceIn(0f, 1f)
        val projX = x1 + clampedT * dx
        val projY = y1 + clampedT * dy
        return distance(px, py, projX, projY)
    }

    private fun distance(x1: Float, y1: Float, x2: Float, y2: Float): Float =
        hypot((x2 - x1).toDouble(), (y2 - y1).toDouble()).toFloat()

    // using this function to check the user position at the POIs

    fun beginUserPath() {
        userDrawnPath.clear()
    }

    fun addUserPathPoint(point: ConfigPoint) {
        userDrawnPath.add(point)

        // Replace last temporary drawn path
        paths.removeAll { it === userDrawnPath }
        paths.add(userDrawnPath)

        invalidate()
    }

    fun clearUserPath() {
        paths.remove(userDrawnPath)
        userDrawnPath.clear()
        invalidate()
    }

}

