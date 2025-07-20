package com.app.wifiharvest.services

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.net.wifi.WifiManager
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import android.util.Log
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import com.app.wifiharvest.LocationHelper
import com.app.wifiharvest.MainActivity
import com.app.wifiharvest.R
import com.app.wifiharvest.SharedWifiViewModel
import com.app.wifiharvest.WifiScanner
import com.app.wifiharvest.WifiScanListener
import com.app.wifiharvest.WifiNetwork
import com.app.wifiharvest.WifiViewModelHolder

class WifiScanService : Service(), ViewModelStoreOwner, WifiScanListener {

    private val channelId = "wifi_scan_channel"
    private val notificationId = 1337

    private lateinit var wifiScanner: WifiScanner
    private lateinit var viewModel: SharedWifiViewModel
    private lateinit var notificationManager: NotificationManager
    private lateinit var locationHelper: LocationHelper
    private var wakeLock: PowerManager.WakeLock? = null

    private var networksFound = 0
    private var scanAttempts = 0

    override val viewModelStore: ViewModelStore = ViewModelStore()

    override fun onCreate() {
        super.onCreate()
        Log.d("WifiScanService", "Service created")

        notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // Acquire wake lock to prevent device from sleeping
        val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
        wakeLock = powerManager.newWakeLock(
            PowerManager.PARTIAL_WAKE_LOCK,
            "WiFiHarvest:WifiScanWakeLock"
        )
        wakeLock?.acquire(60 * 60 * 1000L) // 1 hour max

        startForeground(notificationId, createNotification("Initializing Wi-Fi scanner..."))
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d("WifiScanService", "Service started")

        // Initialize components
        val wifiManager = applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
        locationHelper = LocationHelper(this)

        // Debug: Check ViewModel instance
        val serviceViewModel = WifiViewModelHolder.getViewModel()
        Log.d("WifiScanService", "Service using ViewModel instance: ${serviceViewModel.hashCode()}")
        Log.d("WifiScanService", "Current networks in ViewModel: ${serviceViewModel.networks.value?.size ?: 0}")

        // Store reference for later use
        viewModel = serviceViewModel

        // Check if Wi-Fi is enabled
        if (!wifiManager.isWifiEnabled) {
            Log.w("WifiScanService", "Wi-Fi is disabled")
            updateNotification("Wi-Fi is disabled - enable Wi-Fi to scan")
            return START_STICKY
        }

        wifiScanner = WifiScanner(
            context = applicationContext,
            wifiManager = wifiManager,
            locationHelper = locationHelper,
            viewModel = serviceViewModel, // Pass explicit instance instead of null
            scanListener = this
        )

        wifiScanner.startScanLoop()
        updateNotification("Wi-Fi scanning started - waiting for GPS...")

        // Return START_STICKY to restart service if killed
        return START_STICKY
    }

    override fun onDestroy() {
        Log.d("WifiScanService", "Service being destroyed")

        try {
            wifiScanner.stopScanLoop()
        } catch (e: Exception) {
            Log.e("WifiScanService", "Error stopping scanner", e)
        }

        try {
            locationHelper.stopLocationUpdates()
        } catch (e: Exception) {
            Log.e("WifiScanService", "Error stopping location updates", e)
        }

        wakeLock?.release()

        super.onDestroy()
        Log.d("WifiScanService", "Service destroyed")
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun createNotification(contentText: String): Notification {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "WiFi Scanning",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "WiFiHarvest background scanning"
                setShowBadge(false)
            }
            notificationManager.createNotificationChannel(channel)
        }

        // Create intent to open main activity when notification is tapped
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Notification.Builder(this, channelId)
                .setContentTitle("WiFiHarvest")
                .setContentText(contentText)
                .setSmallIcon(R.drawable.ic_notification)
                .setContentIntent(pendingIntent)
                .setOngoing(true)
                .setAutoCancel(false)
                .build()
        } else {
            @Suppress("DEPRECATION")
            Notification.Builder(this)
                .setContentTitle("WiFiHarvest")
                .setContentText(contentText)
                .setSmallIcon(R.drawable.ic_notification)
                .setContentIntent(pendingIntent)
                .setOngoing(true)
                .setAutoCancel(false)
                .build()
        }
    }

    private fun updateNotification(text: String) {
        val notification = createNotification(text)
        notificationManager.notify(notificationId, notification)
    }

    // ============================
    // WifiScanListener implementation
    // ============================
    override fun onNewNetworkFound(ssid: String, bssid: String, lat: Double, lng: Double) {
        networksFound++
        Log.d("WifiScanService", "New network found: $ssid (Total: $networksFound)")

        // Debug: Verify network was added to ViewModel
        val currentNetworks = viewModel.networks.value?.size ?: 0
        Log.d("WifiScanService", "ViewModel now contains $currentNetworks networks")

        val displaySsid = if (ssid.isBlank()) "Hidden Network" else ssid
        updateNotification("Found: $displaySsid (Total: $networksFound networks)")
    }

    override fun onScanStatusChanged(status: String) {
        scanAttempts++
        Log.d("WifiScanService", "Scan status: $status (Attempt: $scanAttempts)")

        val locationInfo = locationHelper.getLocationInfo()
        val statusText = when {
            !locationHelper.isLocationAvailable() -> "Waiting for GPS location..."
            status.contains("throttled") || status.contains("cached") -> "Android limiting scans - using existing data"
            status.contains("quota") -> status // "Waiting for scan quota reset"
            status.contains("No new networks") -> "Active scanning ($networksFound networks found)"
            networksFound == 0 -> "Scanning for networks..."
            else -> "Scanning ($networksFound found)"
        }

        updateNotification("$statusText • ${locationInfo.substringAfter(": ")}")
    }

    override fun onScanFailed(error: String) {
        Log.w("WifiScanService", "Scan failed: $error")

        val statusText = when {
            error.contains("throttled") -> "Scan rate limited - continuing with cached results"
            error.contains("Location permission") -> "Location permission required"
            !locationHelper.isLocationAvailable() -> "Waiting for GPS fix..."
            else -> "Scan issue - retrying..."
        }

        updateNotification(statusText)
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        Log.d("WifiScanService", "Task removed - restarting service")

        // Restart the service when app is swiped away
        val restartServiceIntent = Intent(applicationContext, WifiScanService::class.java)
        startService(restartServiceIntent)

        super.onTaskRemoved(rootIntent)
    }
}