package com.matt.guidebeacons.activities

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.text.Html
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.punchthrough.blestarterappandroid.databinding.ActivityPermissionsBinding

class PermissionsCheckActivity : AppCompatActivity() {

    private lateinit var binding: ActivityPermissionsBinding

    class Permission(val name: String, val minSdk: Int = Build.VERSION_CODES.BASE, val maxSdk: Int = Build.VERSION_CODES.CUR_DEVELOPMENT)

    private val permissions = listOf(
        Permission(Manifest.permission.BLUETOOTH, maxSdk = Build.VERSION_CODES.R),
        Permission(Manifest.permission.BLUETOOTH_ADMIN, maxSdk = Build.VERSION_CODES.R),
        Permission(Manifest.permission.BLUETOOTH_SCAN, minSdk = Build.VERSION_CODES.S),
        Permission(Manifest.permission.ACCESS_COARSE_LOCATION),
        Permission(Manifest.permission.ACCESS_FINE_LOCATION),
        Permission(Manifest.permission.VIBRATE),
        Permission(Manifest.permission.BLUETOOTH_CONNECT, minSdk = Build.VERSION_CODES.S)
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPermissionsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        populate()
    }

    private fun populate() {
        var out: String = ""

        for (p in permissions) {
            val status = when(Build.VERSION.SDK_INT >= p.minSdk && Build.VERSION.SDK_INT <= p.maxSdk) {
                false -> "n/a"
                else -> when(this.hasPermission(p.name)) {
                    true -> Html.fromHtml("<font color='#16E049'>O</font>")
                    else -> Html.fromHtml("<font color='#0091EA'>X</font>")
                }
            }
            out += "${p.name}\n\t\t${status}\n"
        }

        binding.debugTextView.text = out
    }




    private fun Context.hasPermission(permissionType: String): Boolean {
        return ContextCompat.checkSelfPermission(this, permissionType) == PackageManager.PERMISSION_GRANTED
    }
}