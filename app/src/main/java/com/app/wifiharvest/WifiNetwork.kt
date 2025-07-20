package com.app.wifiharvest

data class WifiNetwork(
    val ssid: String,
    val bssid: String,
    val signal: Int,
    val lat: Double,
    val lng: Double,
    val timestamp: String? = null,
    val location: String? = null
)