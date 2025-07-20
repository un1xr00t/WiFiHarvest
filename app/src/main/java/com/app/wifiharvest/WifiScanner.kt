package com.app.wifiharvest

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.wifi.ScanResult
import android.net.wifi.WifiManager
import kotlinx.coroutines.*
import com.google.android.gms.location.LocationServices
import android.widget.Toast
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import android.util.Log

class WifiScanner(
    private val context: Context,
    private val adapter: WifiLogAdapter,
    private val locationHelper: LocationHelper,
    private val viewModel: SharedWifiViewModel
) {
    private val wifiManager =
        context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
    private var scanJob: Job? = null
    private var scanListener: WifiScanListener? = null
    private val seenBssids = mutableSetOf<String>()

    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(ctx: Context?, intent: Intent?) {
            if (intent?.action == WifiManager.SCAN_RESULTS_AVAILABLE_ACTION) {
                val success = intent.getBooleanExtra(WifiManager.EXTRA_RESULTS_UPDATED, false)
                processScanResults(wifiManager.scanResults)
            }
        }
    }

    private fun processScanResults(results: List<ScanResult>) {
            locationHelper.getFreshLocation { location ->
            if (location == null) return@getFreshLocation

                Log.d("WiFiScan", "GPS: Lat=${location.latitude}, Lng=${location.longitude}")
            Log.d("WiFiScan", "Scan cycle started — found ${results.size} networks")
            Log.d("WiFiScan", "Total unique BSSIDs seen so far: ${seenBssids.size}")

            for (result in results) {
                val ssid = result.SSID ?: "N/A"
                val bssid = result.BSSID
                val rssi = result.level

                if (seenBssids.add(bssid)) {
                    GlobalScope.launch(Dispatchers.Main) {
                        scanListener?.onNewScanResult(ssid, bssid, location.latitude, location.longitude)
                    }

                    locationHelper.getAddressFromLocation(location.latitude, location.longitude) { address ->
                        val resolvedAddress = address ?: "Lat: ${location.latitude}, Lng: ${location.longitude}"

                        val newNetwork = WifiNetwork(
                            ssid = ssid,
                            bssid = bssid,
                            latitude = location.latitude,
                            longitude = location.longitude,
                            address = resolvedAddress
                        )
                        viewModel.addNetwork(newNetwork)

                        GlobalScope.launch(Dispatchers.Main) {
                            adapter.addLog(
                                WifiLogEntry(
                                    ssid = ssid,
                                    bssid = bssid,
                                    timestamp = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date()),
                                    location = resolvedAddress
                                )
                            )
                        }
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

        GlobalScope.launch(Dispatchers.Main) {
            Toast.makeText(context, "Scanning started", Toast.LENGTH_SHORT).show()
        }

        if (scanJob?.isActive == true) return

        scanJob = CoroutineScope(Dispatchers.IO).launch {
            while (isActive) {
                wifiManager.startScan()
                delay(3000)
            }
        }
    }

    fun stopScanning() {
        try {
            context.unregisterReceiver(receiver)
        } catch (e: Exception) {
            println("Error unregistering receiver: ${e.message}")
        }

        GlobalScope.launch(Dispatchers.Main) {
            Toast.makeText(context, "Scanning stopped", Toast.LENGTH_SHORT).show()
        }

        scanJob?.cancel()
        scanJob = null
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
