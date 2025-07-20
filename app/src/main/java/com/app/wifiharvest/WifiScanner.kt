package com.app.wifiharvest

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.location.Location
import android.net.wifi.ScanResult
import android.net.wifi.WifiManager
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.core.app.ActivityCompat
import com.app.wifiharvest.WifiNetwork
import com.app.wifiharvest.SharedWifiViewModel
import kotlin.math.*
import com.app.wifiharvest.WifiScanListener

private const val LOCATION_CLUSTER_RADIUS_METERS = 10f // Reduced from 30m
private const val MIN_SCAN_INTERVAL_MS = 30000L // 30 seconds minimum between scans
private const val MAX_SCAN_ATTEMPTS = 4 // Max scans per 2-minute window
private const val SCAN_RESET_WINDOW_MS = 120000L // 2 minutes in milliseconds

class WifiScanner(
    private val context: Context,
    private val wifiManager: WifiManager,
    private val locationHelper: LocationHelper,
    viewModel: SharedWifiViewModel?, // Can be null - will use singleton
    private val scanListener: WifiScanListener
) {
    private val handler = Handler(Looper.getMainLooper())
    private var scanInterval = MIN_SCAN_INTERVAL_MS

    private val currentLog = mutableListOf<WifiNetwork>()
    private val seenBSSIDs = mutableSetOf<String>()
    private val bssidToLocation = mutableMapOf<String, Pair<Double, Double>>()

    // Use singleton ViewModel
    private val viewModel: SharedWifiViewModel = viewModel ?: WifiViewModelHolder.getViewModel()

    private var isScanning = false
    private var scanAttempts = 0
    private var lastScanResetTime = System.currentTimeMillis()
    private var lastSuccessfulScan = 0L

    // BroadcastReceiver to listen for scan results
    private val wifiScanReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val success = intent.getBooleanExtra(WifiManager.EXTRA_RESULTS_UPDATED, false)
            if (success) {
                Log.d("WifiScanner", "Scan completed successfully")
                scanSuccess()
            } else {
                Log.w("WifiScanner", "Scan failed - using cached results")
                scanFailure()
            }
        }
    }

    fun startScanLoop() {
        if (!isScanning) {
            isScanning = true

            // Register broadcast receiver for scan results
            val intentFilter = IntentFilter()
            intentFilter.addAction(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION)
            context.registerReceiver(wifiScanReceiver, intentFilter)

            handler.post(scanRunnable)
            Log.d("WifiScanner", "WiFi scanning started with ${scanInterval}ms interval")
        }
    }

    fun stopScanLoop() {
        isScanning = false
        handler.removeCallbacks(scanRunnable)

        try {
            context.unregisterReceiver(wifiScanReceiver)
        } catch (e: IllegalArgumentException) {
            // Receiver wasn't registered, ignore
        }

        Log.d("WifiScanner", "WiFi scanning stopped")
    }

    fun getCurrentLog(): List<WifiNetwork> = currentLog.toList()

    private val scanRunnable = object : Runnable {
        override fun run() {
            if (isScanning) {
                attemptScan()
                handler.postDelayed(this, scanInterval)
            }
        }
    }

    private fun attemptScan() {
        // Reset scan attempts counter every 2 minutes
        val currentTime = System.currentTimeMillis()
        if (currentTime - lastScanResetTime > SCAN_RESET_WINDOW_MS) {
            scanAttempts = 0
            lastScanResetTime = currentTime
            scanInterval = MIN_SCAN_INTERVAL_MS // Reset to minimum interval
            Log.d("WifiScanner", "Scan attempts reset, returning to normal interval")
        }

        // Check permissions
        if (ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            Log.w("WifiScanner", "Missing ACCESS_FINE_LOCATION permission")
            scanListener.onScanFailed("Location permission not granted")
            return
        }

        // Always try to process current scan results first (they might be fresh from other apps)
        val currentResults = wifiManager.scanResults
        if (currentResults.isNotEmpty()) {
            Log.d("WifiScanner", "Processing ${currentResults.size} available scan results")
            processResults(currentResults)
        }

        // Only attempt new scan if we haven't exceeded quota
        if (scanAttempts < MAX_SCAN_ATTEMPTS) {
            val success = wifiManager.startScan()
            if (success) {
                scanAttempts++
                lastSuccessfulScan = currentTime
                Log.d("WifiScanner", "Scan initiated successfully (attempt $scanAttempts/$MAX_SCAN_ATTEMPTS)")
            } else {
                Log.w("WifiScanner", "startScan() failed - likely throttled")

                // Increase scan interval to reduce throttling
                scanInterval = minOf(scanInterval * 2, 120000L) // Max 2 minutes
                Log.d("WifiScanner", "Increased scan interval to ${scanInterval}ms due to throttling")

                scanListener.onScanStatusChanged("Scan throttled - using cached results")
            }
        } else {
            val timeUntilReset = SCAN_RESET_WINDOW_MS - (currentTime - lastScanResetTime)
            Log.d("WifiScanner", "Scan quota exceeded, waiting ${timeUntilReset/1000}s for reset")
            scanListener.onScanStatusChanged("Waiting for scan quota reset (${timeUntilReset/1000}s)")
        }
    }

    private fun scanSuccess() {
        // Reset scan interval on successful scan
        scanInterval = MIN_SCAN_INTERVAL_MS
        val results = wifiManager.scanResults
        Log.d("WifiScanner", "Scan completed successfully with ${results.size} results")
        processResults(results)
    }

    private fun scanFailure() {
        // Process cached results even on failure
        val results = wifiManager.scanResults
        Log.d("WifiScanner", "Scan failed but processing ${results.size} cached results")
        if (results.isNotEmpty()) {
            processResults(results)
        }
    }

    private fun processResults(results: List<ScanResult>) {
        val location: Location = locationHelper.getLastKnownLocation() ?: run {
            Log.d("WifiScanner", "No GPS fix yet")
            return
        }

        // Enhanced GPS accuracy logging
        Log.d("WifiScanner", "GPS Status - Accuracy: ${location.accuracy}m, Provider: ${location.provider}, Age: ${(System.currentTimeMillis() - location.time)/1000}s")

        if (location.accuracy > 100f) {
            Log.w("WifiScanner", "REJECTING scan due to GPS accuracy: ${location.accuracy}m > 100m threshold")
            return
        }

        Log.d("WifiScanner", "✓ GPS accuracy acceptable (${location.accuracy}m) - Processing ${results.size} WiFi networks")

        var newNetworksFound = 0
        var duplicatesSkipped = 0
        var locationDuplicatesSkipped = 0

        for (result in results) {
            Log.d("WifiScanner", "Processing network: ${result.SSID} (${result.BSSID})")

            // Skip if we've already seen this BSSID
            if (seenBSSIDs.contains(result.BSSID)) {
                duplicatesSkipped++
                Log.d("WifiScanner", "  → Skipped: Already seen BSSID")
                continue
            }

            val lat = location.latitude
            val lng = location.longitude

            // More intelligent location clustering - only skip if SAME BSSID seen at nearby location
            val existingLocation = bssidToLocation[result.BSSID]
            if (existingLocation != null &&
                isNearby(lat, lng, existingLocation.first, existingLocation.second, LOCATION_CLUSTER_RADIUS_METERS)) {
                locationDuplicatesSkipped++
                Log.d("WifiScanner", "  → Skipped: Same BSSID seen at nearby location (${LOCATION_CLUSTER_RADIUS_METERS}m)")
                continue
            }

            val network = WifiNetwork(
                ssid = result.SSID ?: "",
                bssid = result.BSSID,
                signal = result.level,
                lat = lat,
                lng = lng,
                timestamp = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault()).format(java.util.Date()),
                location = null // Will be geocoded later if needed
            )

            currentLog.add(0, network)
            seenBSSIDs.add(result.BSSID)
            bssidToLocation[result.BSSID] = lat to lng
            newNetworksFound++

            // Limit log size
            if (currentLog.size > 300) currentLog.removeLast()

            viewModel.addNetwork(network)
            Log.d("WifiScanner", "✓ Added network to ViewModel (${viewModel.hashCode()}): ${network.ssid} (${network.bssid})")
            scanListener.onNewNetworkFound(
                network.ssid,
                network.bssid,
                network.lat,
                network.lng
            )
        }

        Log.d("WifiScanner", "SCAN SUMMARY - New: $newNetworksFound, BSSID Dupes: $duplicatesSkipped, Location Dupes: $locationDuplicatesSkipped, Total Unique: ${seenBSSIDs.size}")

        if (newNetworksFound > 0) {
            scanListener.onScanStatusChanged("Found $newNetworksFound new networks")
        } else {
            scanListener.onScanStatusChanged("No new networks found")
        }
    }

    private fun isNearby(
        lat1: Double, lng1: Double,
        lat2: Double, lng2: Double,
        radiusMeters: Float = LOCATION_CLUSTER_RADIUS_METERS
    ): Boolean {
        val results = FloatArray(1)
        Location.distanceBetween(lat1, lng1, lat2, lng2, results)
        return results[0] <= radiusMeters
    }
}