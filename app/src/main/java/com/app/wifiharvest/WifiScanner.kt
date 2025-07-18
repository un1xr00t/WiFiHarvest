package com.app.wifiharvest

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.wifi.WifiManager
import kotlinx.coroutines.*
import java.util.concurrent.CopyOnWriteArrayList

class WifiScanner(
    private val context: Context,
    private val adapter: WifiLogAdapter,
    private val locationHelper: LocationHelper
) {
    private val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private val seenEntries = CopyOnWriteArrayList<WifiLogEntry>()

    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(ctx: Context?, intent: Intent?) {
            val results = wifiManager.scanResults
            locationHelper.getCurrentLocation { location ->
                val lat = location?.latitude
                val lng = location?.longitude

                results.forEach { result ->
                    val entry = WifiLogEntry(
                        ssid = result.SSID,
                        bssid = result.BSSID,
                        latitude = lat,
                        longitude = lng
                    )

                    if (!isDuplicate(entry)) {
                        seenEntries.add(entry)
                        adapter.addEntry(entry)
                    }
                }
            }
        }
    }

    private fun isDuplicate(newEntry: WifiLogEntry): Boolean {
        return seenEntries.any { existing ->
            existing.bssid == newEntry.bssid &&
            existing.latitude != null && newEntry.latitude != null &&
            existing.longitude != null && newEntry.longitude != null &&
            areClose(existing.latitude, newEntry.latitude) &&
            areClose(existing.longitude, newEntry.longitude)
        }
    }

    private fun areClose(a: Double?, b: Double?, threshold: Double = 0.0001): Boolean {
        if (a == null || b == null) return false
        return kotlin.math.abs(a - b) <= threshold
    }

    fun startScanning() {
        context.registerReceiver(receiver, IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION))
        scope.launch {
            while (true) {
                wifiManager.startScan()
                delay(5000)
            }
        }
    }

    fun stopScanning() {
        scope.cancel()
        try {
            context.unregisterReceiver(receiver)
        } catch (_: Exception) {}
    }
}