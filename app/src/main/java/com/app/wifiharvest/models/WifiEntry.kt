// app/src/main/java/com/app/wifiharvest/models/WifiEntry.kt
package com.app.wifiharvest.models

data class WifiEntry(
    val SSID: String,
    val BSSID: String,
    val lat: Double,
    val lng: Double,
    val signal: Int
)
