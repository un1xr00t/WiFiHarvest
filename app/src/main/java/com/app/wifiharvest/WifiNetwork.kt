package com.app.wifiharvest

data class WifiNetwork(
    val ssid: String,
    val bssid: String,
    val latitude: Double,
    val longitude: Double
)