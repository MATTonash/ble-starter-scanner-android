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
import android.speech.tts.TextToSpeech
import android.view.MotionEvent


data class ConfigPoint(val x: Float, val y: Float)

data class UserMapConfig(
    val beacons: List<ConfigPoint> = emptyList(),
    val polygons: List<List<ConfigPoint>> = emptyList(),
    val paths: List<List<ConfigPoint>> = emptyList(),
    val startRectangles: List<List<ConfigPoint>> = emptyList(),
    val endRectangles: List<List<ConfigPoint>> = emptyList(),
    val maxX: Float = 5f,
    val maxY: Float = 5f
)



class UserMapView(context: Context, attrs: AttributeSet? = null) : View(context, attrs), TextToSpeech.OnInitListener {

    // Logical coordinate system
    private var maxX = 5f
    private var maxY = 5f

    // Screen mapping
    private var scale = 1f
    private var offsetX = 0f
    private var offsetY = 0f

    // Paints
    private val screenBackgroundPaint = Paint().apply { color = Color.DKGRAY; style = Paint.Style.FILL }
    private val mapBackgroundPaint = Paint().apply { color = Color.LTGRAY; style = Paint.Style.FILL }
    private val beaconPaint = Paint().apply { color = Color.RED; style = Paint.Style.FILL; alpha = 200 }
    private val polygonPaint = Paint().apply { color = Color.BLUE; style = Paint.Style.FILL; alpha = 100 }
    private val linePaint = Paint().apply { color = Color.GREEN; style = Paint.Style.STROKE; strokeWidth = 20f }
    private val startRectPaint = Paint().apply { color = Color.YELLOW; style = Paint.Style.FILL }
    private val endRectPaint = Paint().apply { color = Color.MAGENTA; style = Paint.Style.FILL }
    private val userPaint = Paint().apply { color = Color.CYAN; style = Paint.Style.FILL }

    private var userPosition: ConfigPoint? = null
    private val beacons = mutableListOf<ConfigPoint>()
    private val polygons = mutableListOf<List<ConfigPoint>>()
    private val paths = mutableListOf<List<ConfigPoint>>()
    private val startRectangles = mutableListOf<List<ConfigPoint>>()
    private val endRectangles = mutableListOf<List<ConfigPoint>>()


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


    /** Load XML configuration from res/raw */
    fun loadConfigFromRawXml(@RawRes resId: Int) {
        context.resources.openRawResource(resId).use { inputStream ->
            val parser = Xml.newPullParser()
            parser.setInput(inputStream, null)
            val config = parseXmlConfigFromParser(parser)
            applyConfig(config)
        }
    }

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

    private fun parseXmlConfigFromParser(parser: XmlPullParser): UserMapConfig {
        val beacons = mutableListOf<ConfigPoint>()
        val polygons = mutableListOf<List<ConfigPoint>>()
        val paths = mutableListOf<List<ConfigPoint>>()
        val startRects = mutableListOf<List<ConfigPoint>>()
        val endRects = mutableListOf<List<ConfigPoint>>()

        var currentPolygon: MutableList<ConfigPoint>? = null
        var currentPath: MutableList<ConfigPoint>? = null
        var maxX = 5f
        var maxY = 5f

        var eventType = parser.eventType
        while (eventType != XmlPullParser.END_DOCUMENT) {
            when (eventType) {
                XmlPullParser.START_TAG -> {
                    when (parser.name) {
                        "coordinateSystem" -> {
                            maxX = parser.getAttributeValue(null, "maxX")?.toFloat() ?: 5f
                            maxY = parser.getAttributeValue(null, "maxY")?.toFloat() ?: 5f
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
                                currentPolygon != null -> currentPolygon!!.add(ConfigPoint(x, y))
                                currentPath != null -> currentPath!!.add(ConfigPoint(x, y))
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

        PoiHandler.handleUserAtPOI(
            user = userPosition,
            startRectangle = startRectangles,
            endRectangles = endRectangles,
            paths = paths,
            onStart = { speak("Hello!, You are at Starting Position") },
            onEnd = { speak("You have Reached Your Destination")},
            onPath = { /* Action when on path */ },
            onNone = { /* Action when at none */ }
        )
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
            canvas.drawCircle(px, py, 0.25f * scale, userPaint)
        }

    }

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

    private fun distance(x1: Float, y1: Float, x2: Float, y2: Float): Float =
        hypot((x2 - x1).toDouble(), (y2 - y1).toDouble()).toFloat()

    // using this function to check the user position at the POIs

}

