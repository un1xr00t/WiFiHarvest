package com.app.wifiharvest.models

data class WifiEntry(
    val SSID: String,
    val BSSID: String,
    val lat: Double,
    val lng: Double,
    val signal: Int,
    val location: String? = null,
    val timestamp: String? = null  // ✅ Optional timestamp field
)
