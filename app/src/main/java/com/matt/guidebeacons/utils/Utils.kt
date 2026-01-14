package com.matt.guidebeacons.utils

private val macRegex = Regex("([0-9a-fA-F]{2}:){5}[0-9a-fA-F]{2}")
fun isMacAddress(str: String) = str.matches(macRegex)
