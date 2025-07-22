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

    private val channelId = "wifi_recon_channel"
    private val notificationId = 1337

    private lateinit var wifiScanner: WifiScanner
    private lateinit var viewModel: SharedWifiViewModel
    private lateinit var notificationManager: NotificationManager

    // Make locationHelper nullable to fix initialization issue
    private var locationHelper: LocationHelper? = null
    private var wakeLock: PowerManager.WakeLock? = null

    private var networksFound = 0
    private var scanAttempts = 0
    private var scanStartTime: Long = System.currentTimeMillis()

    override val viewModelStore: ViewModelStore = ViewModelStore()

    override fun onCreate() {
        super.onCreate()
        Log.d("WifiScanService", "🤖 INITIATING MR.ROBOT PROTOCOL")

        notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // Acquire wake lock to prevent device from sleeping
        val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
        wakeLock = powerManager.newWakeLock(
            PowerManager.PARTIAL_WAKE_LOCK,
            "WiFiHarvest:ReconWakeLock"
        )
        wakeLock?.acquire(60 * 60 * 1000L) // 1 hour max

        // Initialize locationHelper here to prevent UninitializedPropertyAccessException
        locationHelper = LocationHelper(this)

        startForeground(notificationId, createMrRobotNotification("INITIALIZING RECONNAISSANCE PROTOCOL..."))
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // Handle stop action from notification
        if (intent?.action == "STOP_SCAN") {
            Log.d("WifiScanService", "🛑 ABORT SIGNAL RECEIVED FROM NOTIFICATION")

            // Stop the scanner first
            try {
                if (::wifiScanner.isInitialized) {
                    wifiScanner.stopScanLoop()
                    Log.d("WifiScanService", "🔚 Scanner stopped")
                }
            } catch (e: Exception) {
                Log.e("WifiScanService", "❌ Error stopping scanner", e)
            }

            // Clear notification and stop service
            notificationManager.cancel(notificationId)
            stopForeground(true)
            stopSelf()

            Log.d("WifiScanService", "💀 SERVICE TERMINATED VIA NOTIFICATION")
            return START_NOT_STICKY
        }

        scanStartTime = System.currentTimeMillis()
        Log.d("WifiScanService", "🎯 RECONNAISSANCE OPERATION STARTED")

        // Initialize components
        val wifiManager = applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager

        // locationHelper should already be initialized in onCreate()
        val helper = locationHelper ?: run {
            Log.e("WifiScanService", "❌ LocationHelper not initialized, creating new instance")
            LocationHelper(this).also { locationHelper = it }
        }

        // Debug: Check ViewModel instance
        val serviceViewModel = WifiViewModelHolder.getViewModel()
        Log.d("WifiScanService", "📡 Service using ViewModel instance: ${serviceViewModel.hashCode()}")
        Log.d("WifiScanService", "📊 Current targets in ViewModel: ${serviceViewModel.networks.value?.size ?: 0}")

        // Store reference for later use
        viewModel = serviceViewModel

        // Check if Wi-Fi is enabled
        if (!wifiManager.isWifiEnabled) {
            Log.w("WifiScanService", "⚠️ WIFI_DISABLED - ENABLE TO PROCEED")
            updateNotification("WIFI_DISABLED • ENABLE_WIFI_TO_SCAN")
            return START_STICKY
        }

        wifiScanner = WifiScanner(
            context = applicationContext,
            wifiManager = wifiManager,
            locationHelper = helper,
            viewModel = serviceViewModel,
            scanListener = this
        )

        wifiScanner.startScanLoop()
        updateNotification("DEEP_PACKET_ANALYSIS_INITIATED • GPS_LOCK_ACQUIRING...")

        // Return START_STICKY to restart service if killed
        return START_STICKY
    }

    override fun onDestroy() {
        Log.d("WifiScanService", "🔚 TERMINATING RECONNAISSANCE OPERATION")

        // Clear the persistent notification when service stops
        notificationManager.cancel(notificationId)

        try {
            if (::wifiScanner.isInitialized) {
                wifiScanner.stopScanLoop()
            }
        } catch (e: Exception) {
            Log.e("WifiScanService", "❌ Error stopping scanner", e)
        }

        try {
            locationHelper?.stopLocationUpdates()
        } catch (e: Exception) {
            Log.e("WifiScanService", "❌ Error stopping location updates", e)
        }

        wakeLock?.release()

        super.onDestroy()
        Log.d("WifiScanService", "💀 RECONNAISSANCE SERVICE TERMINATED")
    }

    override fun onBind(intent: Intent?): IBinder? = null

    // ============================
    // MR. ROBOT NOTIFICATION SYSTEM
    // ============================

    private fun createMrRobotNotification(contentText: String): Notification {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "WiFi Reconnaissance Operations",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Background network reconnaissance operations"
                setShowBadge(false)
                lockscreenVisibility = Notification.VISIBILITY_PUBLIC
                enableLights(false)
                enableVibration(false)
                setSound(null, null)
            }
            notificationManager.createNotificationChannel(channel)
        }

        // Create stop action intent
        val stopIntent = Intent(this, WifiScanService::class.java).apply {
            action = "STOP_SCAN"
        }
        val stopPendingIntent = PendingIntent.getService(
            this, 1, stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Create view app intent
        val viewIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val viewPendingIntent = PendingIntent.getActivity(
            this, 0, viewIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val lockScreenTitle = "WiFiHarvest • RECON ACTIVE"

        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Notification.Builder(this, channelId)
                .setContentTitle(lockScreenTitle)
                .setContentText(formatLockScreenMessage(contentText))
                .setSmallIcon(R.drawable.ic_notification)
                .setContentIntent(viewPendingIntent)
                .setOngoing(true)
                .setAutoCancel(false)
                .setShowWhen(true)
                .setWhen(scanStartTime)
                .addAction(R.drawable.ic_notification, "ABORT", stopPendingIntent)
                .addAction(R.drawable.ic_notification, "VIEW", viewPendingIntent)
                .setColor(android.graphics.Color.parseColor("#00FF41"))
                .setCategory(Notification.CATEGORY_SERVICE)
                .setPriority(Notification.PRIORITY_LOW)
                .setStyle(Notification.BigTextStyle()
                    .bigText(formatExpandedLockScreenMessage(contentText))
                    .setBigContentTitle(lockScreenTitle))
                .build()
        } else {
            @Suppress("DEPRECATION")
            Notification.Builder(this)
                .setContentTitle(lockScreenTitle)
                .setContentText(formatLockScreenMessage(contentText))
                .setSmallIcon(R.drawable.ic_notification)
                .setContentIntent(viewPendingIntent)
                .setOngoing(true)
                .setAutoCancel(false)
                .setShowWhen(true)
                .setWhen(scanStartTime)
                .addAction(R.drawable.ic_notification, "ABORT", stopPendingIntent)
                .addAction(R.drawable.ic_notification, "VIEW", viewPendingIntent)
                .setPriority(Notification.PRIORITY_LOW)
                .build()
        }
    }

    private fun updateNotification(text: String) {
        val notification = createMrRobotNotification(text)
        notificationManager.notify(notificationId, notification)
    }

    // Lock screen message formatting - Short and hacker-style
    private fun formatLockScreenMessage(originalText: String): String {
        val timeStr = formatScanTime()

        return when {
            originalText.contains("INITIALIZING") ->
                "System loading... • $timeStr"

            originalText.contains("GPS") && networksFound == 0 ->
                "Acquiring coordinates... • $timeStr"

            networksFound == 0 ->
                "Scanning networks... • $timeStr"

            networksFound > 0 ->
                "$networksFound targets found • $timeStr"

            originalText.contains("throttled") ->
                "Bypassing restrictions... • $timeStr"

            originalText.contains("WIFI_DISABLED") ->
                "WiFi module offline"

            else -> "Deep scan active • $timeStr"
        }
    }

    // Expanded lock screen message when user pulls down notification
    private fun formatExpandedLockScreenMessage(originalText: String): String {
        val elapsedTime = formatScanTime()
        val locationStatus = locationHelper?.let { helper ->
            if (helper.isLocationAvailable()) "GPS_LOCKED" else "GPS_ACQUIRING"
        } ?: "GPS_OFFLINE"

        val operationPhase = when {
            networksFound == 0 -> "RECONNAISSANCE"
            networksFound < 10 -> "TARGET_ACQUISITION"
            networksFound < 25 -> "DEEP_SCAN_ACTIVE"
            networksFound < 50 -> "HIGH_VALUE_ZONE"
            else -> "SATURATION_MODE"
        }

        return """
🔍 OPERATION: $operationPhase
📡 TARGETS: $networksFound discovered
⏱️ RUNTIME: $elapsedTime
📍 GPS: $locationStatus
🎯 STATUS: Running in background...

Tap to view full reconnaissance data.
Use ABORT to terminate operation.
        """.trimIndent()
    }

    private fun formatScanTime(): String {
        val elapsedMillis = System.currentTimeMillis() - scanStartTime
        val seconds = (elapsedMillis / 1000) % 60
        val minutes = (elapsedMillis / (1000 * 60)) % 60
        val hours = (elapsedMillis / (1000 * 60 * 60))
        return String.format("%02d:%02d:%02d", hours, minutes, seconds)
    }

    private fun extractSSIDFromText(text: String): String {
        val patterns = listOf(
            "Found: (.+?) \\(".toRegex(),
            "TARGET_ACQUIRED: (.+?) •".toRegex(),
            "TARGET_ACQUIRED: (.+?)$".toRegex()
        )

        for (pattern in patterns) {
            val match = pattern.find(text)
            if (match != null) {
                return match.groupValues[1].take(15)
            }
        }
        return "UNKNOWN_NET"
    }

    private fun extractCoordinatesFromLocation(locationInfo: String): String {
        val coordsPattern = "\\[([-\\d.]+)°[NS], ([-\\d.]+)°[EW]\\]".toRegex()
        val match = coordsPattern.find(locationInfo)
        return match?.groupValues?.let {
            "${it[1].take(6)}, ${it[2].take(6)}"
        } ?: "ACQUIRING"
    }

    private fun getLastSignalStrength(): Int {
        return if (::viewModel.isInitialized) {
            viewModel.networks.value?.lastOrNull()?.signal ?: -70
        } else {
            -70
        }
    }

    // ============================
    // ENHANCED WIFISCANLISTENER IMPLEMENTATION
    // ============================

    override fun onNewNetworkFound(ssid: String, bssid: String, lat: Double, lng: Double) {
        Log.d("WifiScanService", "🎯 NEW TARGET ACQUIRED: $ssid")

        // Get ACTUAL count from ViewModel (the real count)
        if (::viewModel.isInitialized) {
            val actualNetworkCount = viewModel.networks.value?.size ?: 0
            networksFound = actualNetworkCount // Sync our counter with reality
            Log.d("WifiScanService", "📊 ViewModel contains $actualNetworkCount networks (using this as real count)")
        } else {
            networksFound++ // Fallback if ViewModel not ready
            Log.d("WifiScanService", "⚠️ ViewModel not initialized, using basic counter: $networksFound")
        }

        val displaySsid = if (ssid.isBlank()) "HIDDEN_TARGET" else ssid.uppercase()
        val signalStrength = getLastSignalStrength()

        updateNotification("TARGET_ACQUIRED: $displaySsid • SIGNAL: ${signalStrength}dBm")

        Log.d("WifiScanService", "🎯 Final target count: $networksFound")
    }

    override fun onScanStatusChanged(status: String) {
        scanAttempts++
        Log.d("WifiScanService", "📡 SCAN_STATUS: $status (Attempt: $scanAttempts)")

        val locationInfo = locationHelper?.getLocationInfo() ?: "GPS not available"
        val statusText = when {
            locationHelper?.isLocationAvailable() != true -> "GPS_LOCK: [ACQUIRING...]"
            status.contains("throttled") || status.contains("cached") -> "SCAN_THROTTLED • USING_CACHED_DATA"
            status.contains("quota") -> "QUOTA_EXCEEDED • WAITING_FOR_RESET"
            status.contains("No new networks") -> "DEEP_SCAN_ACTIVE • $networksFound TARGETS"
            networksFound == 0 -> "RECONNAISSANCE_INITIATED..."
            else -> "ACTIVE_SCAN • $networksFound TARGETS_ACQUIRED"
        }

        val coords = extractCoordinatesFromLocation(locationInfo)
        updateNotification("$statusText • GPS: [$coords]")
    }

    override fun onScanFailed(error: String) {
        Log.w("WifiScanService", "⚠️ SCAN_FAILURE: $error")

        val statusText = when {
            error.contains("throttled") -> "RATE_LIMITED • CONTINUING_WITH_CACHED"
            error.contains("Location permission") -> "GPS_ACCESS_DENIED • PERMISSION_REQUIRED"
            locationHelper?.isLocationAvailable() != true -> "GPS_SIGNAL_LOST • REACQUIRING..."
            else -> "SCAN_ERROR • RETRY_INITIATED"
        }

        updateNotification(statusText)
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        Log.d("WifiScanService", "🔄 APP_TASK_REMOVED - CONTINUING_BACKGROUND_RECON")

        val restartServiceIntent = Intent(applicationContext, WifiScanService::class.java)
        startService(restartServiceIntent)

        super.onTaskRemoved(rootIntent)
    }
}