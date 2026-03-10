/*
 * Copyright 2024 Punch Through Design LLC
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

import android.Manifest
import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import timber.log.Timber

/**
 * Determine whether the current [Context] has been granted the relevant [Manifest.permission].
 */
fun Context.hasPermission(permissionType: String): Boolean {
    return ContextCompat.checkSelfPermission(this, permissionType) ==
        PackageManager.PERMISSION_GRANTED
}

/**
 * Determine whether the current [Context] has been granted the relevant permissions to perform
 * beacon scanning operations depending on the mobile device's Android version.
 */
fun Context.hasRequiredRuntimePermissions(): Boolean {
    return hasLocationPermission() && hasNearbyDevicesPermission()
}

/**
 * Request for the necessary permissions for beacon scanning operations to work.
 */
fun Activity.requestRequiredRuntimePermissions(requestCode: Int) {
    if (hasRequiredRuntimePermissions()) {
        Timber.w("Required runtime permission(s) already granted")
        return
    }

    if (!hasNearbyDevicesPermission()) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (nearbyDevicesPermissionRationaleRequired()) {
                displayNearbyDevicesPermissionRationale(requestCode)
            } else {
                requestNearbyDevicesPermissions(requestCode)
            }
        }
    }

    if (!hasLocationPermission()) {
        if (locationPermissionRationaleRequired()) {
            displayLocationPermissionRationale(requestCode)
        } else {
            requestLocationPermission(requestCode)
        }
    }
}

//region Location permission
private fun Context.hasLocationPermission(): Boolean {
    return hasPermission(Manifest.permission.ACCESS_FINE_LOCATION)
}

private fun Activity.locationPermissionRationaleRequired(): Boolean {
    return ActivityCompat.shouldShowRequestPermissionRationale(
        this,
        Manifest.permission.ACCESS_FINE_LOCATION
    )
}

private fun Activity.displayLocationPermissionRationale(requestCode: Int) {
    runOnUiThread {
        AlertDialog.Builder(this)
            .setTitle(R.string.location_permission_required)
            .setMessage(R.string.location_permission_rationale)
            .setPositiveButton(android.R.string.ok) { _, _ ->
                requestLocationPermission(requestCode)
            }
            .setNegativeButton(R.string.quit) { _, _ -> finishAndRemoveTask() }
            .setCancelable(false)
            .show()
    }
}

private fun Activity.requestLocationPermission(requestCode: Int) {
    ActivityCompat.requestPermissions(
        this,
        arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
        requestCode
    )
}
//endregion

//region Nearby Devices permissions
private fun Context.hasNearbyDevicesPermission(): Boolean {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        return hasPermission(Manifest.permission.BLUETOOTH_SCAN)
            && hasPermission(Manifest.permission.BLUETOOTH_CONNECT)
    }
    else return true
}

private fun Activity.nearbyDevicesPermissionRationaleRequired(): Boolean {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        ActivityCompat.shouldShowRequestPermissionRationale(
            this, Manifest.permission.BLUETOOTH_SCAN
        ) || ActivityCompat.shouldShowRequestPermissionRationale(
            this, Manifest.permission.BLUETOOTH_CONNECT
        )
    } else {
        false
    }
}

@RequiresApi(Build.VERSION_CODES.S)
private fun Activity.displayNearbyDevicesPermissionRationale(requestCode: Int) {
    runOnUiThread {
        AlertDialog.Builder(this)
            .setTitle(R.string.bluetooth_permission_required)
            .setMessage(R.string.bluetooth_permission_rationale)
            .setPositiveButton(android.R.string.ok) { _, _ ->
                requestNearbyDevicesPermissions(requestCode)
            }
            .setNegativeButton(R.string.quit) { _, _ -> finishAndRemoveTask() }
            .setCancelable(false)
            .show()
    }
}

@RequiresApi(Build.VERSION_CODES.S)
private fun Activity.requestNearbyDevicesPermissions(requestCode: Int) {
    ActivityCompat.requestPermissions(
        this,
        arrayOf(
            Manifest.permission.BLUETOOTH_SCAN,
            Manifest.permission.BLUETOOTH_CONNECT
        ),
        requestCode
    )
}
//endregion
