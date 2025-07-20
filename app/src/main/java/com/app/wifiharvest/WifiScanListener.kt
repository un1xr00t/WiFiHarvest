package com.app.wifiharvest

interface WifiScanListener {
    fun onNewNetworkFound(ssid: String, bssid: String, lat: Double, lng: Double)  // Changed lon to lng
    fun onScanStatusChanged(status: String)
    fun onScanFailed(error: String)
}