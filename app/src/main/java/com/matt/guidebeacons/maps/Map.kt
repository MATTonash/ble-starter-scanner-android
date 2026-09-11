package com.matt.guidebeacons.maps

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory

class Map(
    mapName: String,
    beaconDataFileName: String,
    pixelsPerMetre: Int,
    drawableResourceId: Int,
    context: Context
    ) {

    private val mapName = mapName
    private val beaconDataFileName = beaconDataFileName
    private val pixelsPerMetre = pixelsPerMetre
    private val drawableResourceName = context.resources.getResourceEntryName(drawableResourceId)

    private val context = context

    fun getName(): String {
        return mapName
    }

    fun getBeaconDataFileName(): String {
        return beaconDataFileName
    }

    fun getDrawableResourceId(): Int {
        return context.resources.getIdentifier(drawableResourceName, "drawable", context.packageName)
    }

    fun getBitmap(): Bitmap {
        return BitmapFactory.decodeResource(context.resources, getDrawableResourceId())
    }

    fun getPixelsPerMetre(): Int {
        return pixelsPerMetre
    }
}