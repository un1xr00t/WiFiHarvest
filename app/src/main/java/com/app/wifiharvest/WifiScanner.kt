package com.app.wifiharvest

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.wifi.ScanResult
import android.net.wifi.WifiManager
import kotlinx.coroutines.*
import com.google.android.gms.location.LocationServices

class WifiScanner(
    private val context: Context,
    private val adapter: WifiLogAdapter,
    private val locationHelper: LocationHelper,
    private val viewModel: SharedWifiViewModel
) {
    private val wifiManager =
        context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
    private val scope = CoroutineScope(Dispatchers.IO + Job())
    private var scanListener: WifiScanListener? = null
    private val seenBssids = mutableSetOf<String>()

    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(ctx: Context?, intent: Intent?) {
            if (intent?.action == WifiManager.SCAN_RESULTS_AVAILABLE_ACTION) {
                val success = intent.getBooleanExtra(WifiManager.EXTRA_RESULTS_UPDATED, false)
                if (success) {
                    processScanResults(wifiManager.scanResults)
                } else {
                    processScanResults(wifiManager.scanResults) // fallback
                }
            }
        }
    }

    private fun processScanResults(results: List<ScanResult>) {
        locationHelper.getCurrentLocation { location ->
            if (location == null) return@getCurrentLocation

            for (result in results) {
                if (seenBssids.add(result.BSSID)) {
                    val ssid = result.SSID ?: "N/A"
                    val bssid = result.BSSID

                    // Notify map fragment
                    GlobalScope.launch(Dispatchers.Main) {
                        scanListener?.onNewScanResult(ssid, bssid, location.latitude, location.longitude)
                    }

                    // Update ViewModel
                    val newNetwork = WifiNetwork(ssid, bssid, location.latitude, location.longitude)
                    viewModel.addNetwork(newNetwork)

                    // Update log UI
                    GlobalScope.launch(Dispatchers.Main) {
                        adapter.addLog(
                            WifiLogEntry(
                                ssid = ssid,
                                bssid = bssid,
                                timestamp = java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.getDefault()).format(java.util.Date()),
                                location = "Lat: ${location.latitude}, Lng: ${location.longitude}"
                            )
                        )
                    }
                }
            }
        }
    }

    fun setScanListener(listener: WifiScanListener?) {
        scanListener = listener
    }

    fun startScanning() {
        val intentFilter = IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION)
        context.registerReceiver(receiver, intentFilter)
        scope.launch {
            while (isActive) {
                wifiManager.startScan()
                delay(5000)
            }
        }
    }

    fun stopScanning() {
        try {
            context.unregisterReceiver(receiver)
        } catch (e: Exception) {
            println("Error unregistering receiver: ${e.message}")
        }
        scope.cancel()
    }

    fun pushLastScanToListener() {
        val latestNetworks = viewModel.networks.value
        latestNetworks?.lastOrNull()?.let { last ->
            GlobalScope.launch(Dispatchers.Main) {
                scanListener?.onNewScanResult(last.ssid, last.bssid, last.latitude, last.longitude)
            }
        }
    }
}
