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
            // todo: data validation user feedback
            val macAddress: String = binding.macAddressInputField.text.toString()

            if (isMacAddress(macAddress)) {
                val returnIntent = Intent()
                // todo: mac address as keys means that it is case-sensitive; force to upper?
                returnIntent.putExtra(INTENT_EXTRA_SELECTED_BEACON_MAC, macAddress)
                setResult(RESULT_OK, returnIntent)
                finish()
            }
        }
    }

    private fun updateSubmitButtonEnabled() {
        binding.macAddressInputSubmit.isEnabled = isMacAddress(binding.macAddressInputField.text.toString())
    }
}