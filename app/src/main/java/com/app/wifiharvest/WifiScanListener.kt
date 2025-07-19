package com.app.wifiharvest

fun interface WifiScanListener {
    fun onNewScanResult(ssid: String, bssid: String, lat: Double, lon: Double)
}