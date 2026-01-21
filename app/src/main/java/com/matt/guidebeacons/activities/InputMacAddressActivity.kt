package com.matt.guidebeacons.activities

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.addTextChangedListener
import com.matt.guidebeacons.constants.*
import com.matt.guidebeacons.utils.*
import com.punchthrough.blestarterappandroid.databinding.MacAddressInputBinding

class InputMacAddressActivity : AppCompatActivity() {
    private lateinit var binding: MacAddressInputBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = MacAddressInputBinding.inflate(layoutInflater)
        setContentView(binding.root)

        updateSubmitButtonEnabled()
        binding.macAddressInputField.addTextChangedListener { updateSubmitButtonEnabled() }
        binding.macAddressInputSubmit.setOnClickListener {
            val macAddress: String = binding.macAddressInputField.text.toString()

            if (isMacAddress(macAddress)) {
                val returnIntent = Intent()
                // Force to upper to match [android.bluetooth.BluetoothDevice.getAddress]
                returnIntent.putExtra(INTENT_EXTRA_SELECTED_BEACON_MAC, macAddress.uppercase())
                setResult(RESULT_OK, returnIntent)
                finish()
            }
        }
    }

    private fun updateSubmitButtonEnabled() {
        binding.macAddressInputSubmit.isEnabled = isMacAddress(binding.macAddressInputField.text.toString())
    }
}