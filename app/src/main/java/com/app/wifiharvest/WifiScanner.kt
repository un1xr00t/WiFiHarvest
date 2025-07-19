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
    private val wifiManager =
        context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private val seenEntries = CopyOnWriteArrayList<WifiLogEntry>()

    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(ctx: Context?, intent: Intent?) {
            val results = wifiManager.scanResults

            locationHelper.getCurrentLocation { location ->
                val lat = location?.latitude
                val lng = location?.longitude

                if (lat != null && lng != null) {
                    locationHelper.getAddressFromLocation(lat, lng) { address ->
                        results.forEach { result ->
                            val entry = WifiLogEntry(
                                ssid = result.SSID,
                                bssid = result.BSSID,
                                timestamp = getCurrentTime(),
                                location = address ?: "Unknown location"
                            )


                            if (!isDuplicate(entry)) {
                                seenEntries.add(entry)
                                adapter.addLog(entry)
                            }
                        }
                    }
                }
            }
        }
    }

    private fun isDuplicate(newEntry: WifiLogEntry): Boolean {
        if (newEntry.location.isNullOrBlank()) return false

        return seenEntries.any { existing ->
            existing.bssid == newEntry.bssid &&
                    existing.location == newEntry.location
        }
    }

    fun startScanning() {
        context.registerReceiver(
            receiver,
            IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION)
        )
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
        } catch (_: Exception) {
        }
    }
}

private fun getCurrentTime(): String {
    val sdf = java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.getDefault())
    return sdf.format(java.util.Date())
}