package com.app.wifiharvest.models

data class ScanSession(
    val timestamp: String,
    val networks: List<WifiEntry>
)
