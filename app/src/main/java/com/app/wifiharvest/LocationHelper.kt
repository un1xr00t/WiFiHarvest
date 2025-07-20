package com.app.wifiharvest

import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.Manifest
import android.os.Bundle
import android.util.Log
import androidx.core.app.ActivityCompat
import com.google.android.gms.location.*
import android.location.Geocoder
import kotlinx.coroutines.*
import java.util.Locale
import android.os.Looper
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

class LocationHelper(private val context: Context) : LocationListener {

    private val fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)
    private val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager

    private var currentLocation: Location? = null
    private var locationCallback: LocationCallback? = null
    private var isRequestingUpdates = false

    // Legacy LocationManager listener for backup
    private var legacyLocationListener: LocationListener? = null

    init {
        startLocationUpdates()
    }

    @SuppressLint("MissingPermission")
    private fun startLocationUpdates() {
        if (!hasLocationPermission()) {
            Log.w("LocationHelper", "Location permission not granted")
            return
        }

        if (isRequestingUpdates) {
            Log.d("LocationHelper", "Location updates already started")
            return
        }

        // Try Fused Location Provider first (more reliable)
        val locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 10000L)
            .setMinUpdateIntervalMillis(5000L)
            .setMaxUpdateDelayMillis(15000L)
            .build()

        locationCallback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                result.lastLocation?.let { location ->
                    currentLocation = location
                    Log.d("LocationHelper", "🌍 GPS Update: ${location.latitude}, ${location.longitude} (accuracy: ${location.accuracy}m, provider: ${location.provider})")
                }
            }

            override fun onLocationAvailability(availability: LocationAvailability) {
                Log.d("LocationHelper", "Location availability: ${availability.isLocationAvailable}")
                if (!availability.isLocationAvailable) {
                    // Fall back to legacy LocationManager
                    startLegacyLocationUpdates()
                }
            }
        }

        try {
            fusedLocationClient.requestLocationUpdates(
                locationRequest,
                locationCallback!!,
                Looper.getMainLooper()
            )
            isRequestingUpdates = true
            Log.d("LocationHelper", "Started fused location updates")

            // Also try to get last known location immediately
            fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                if (location != null && currentLocation == null) {
                    currentLocation = location
                    Log.d("LocationHelper", "Got cached location: ${location.latitude}, ${location.longitude} (accuracy: ${location.accuracy}m)")
                }
            }
        } catch (e: Exception) {
            Log.e("LocationHelper", "Failed to start fused location updates", e)
            startLegacyLocationUpdates()
        }
    }

    @SuppressLint("MissingPermission")
    private fun startLegacyLocationUpdates() {
        if (!hasLocationPermission()) return

        Log.d("LocationHelper", "Starting legacy LocationManager updates")

        try {
            // Try GPS provider first
            if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                locationManager.requestLocationUpdates(
                    LocationManager.GPS_PROVIDER,
                    10000L, // 10 seconds
                    10f,    // 10 meters
                    this
                )
                Log.d("LocationHelper", "Started GPS location updates")
            }

            // Also try network provider as backup
            if (locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
                locationManager.requestLocationUpdates(
                    LocationManager.NETWORK_PROVIDER,
                    15000L, // 15 seconds
                    50f,    // 50 meters
                    this
                )
                Log.d("LocationHelper", "Started network location updates")
            }

            // Get last known locations from both providers
            val gpsLocation = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)
            val networkLocation = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)

            // Use the more accurate one
            val bestLocation = when {
                gpsLocation != null && networkLocation != null -> {
                    if (gpsLocation.accuracy < networkLocation.accuracy) gpsLocation else networkLocation
                }
                gpsLocation != null -> gpsLocation
                networkLocation != null -> networkLocation
                else -> null
            }

            if (bestLocation != null && currentLocation == null) {
                currentLocation = bestLocation
                Log.d("LocationHelper", "Got legacy cached location: ${bestLocation.latitude}, ${bestLocation.longitude}")
            }

        } catch (e: Exception) {
            Log.e("LocationHelper", "Failed to start legacy location updates", e)
        }
    }

    fun getLastKnownLocation(): Location? {
        val location = currentLocation
        if (location != null) {
            Log.d("LocationHelper", "Returning location: ${location.latitude}, ${location.longitude} (accuracy: ${location.accuracy}m, age: ${(System.currentTimeMillis() - location.time)/1000}s)")
        } else {
            Log.d("LocationHelper", "No location available yet")
        }
        return location
    }

    fun stopLocationUpdates() {
        locationCallback?.let {
            fusedLocationClient.removeLocationUpdates(it)
        }

        try {
            locationManager.removeUpdates(this)
        } catch (e: Exception) {
            Log.w("LocationHelper", "Error removing location updates", e)
        }

        isRequestingUpdates = false
        Log.d("LocationHelper", "Stopped location updates")
    }

    // LocationListener implementation for legacy LocationManager
    override fun onLocationChanged(location: Location) {
        if (isBetterLocation(location, currentLocation)) {
            currentLocation = location
            Log.d("LocationHelper", "🌍 Legacy location update: ${location.latitude}, ${location.longitude} (accuracy: ${location.accuracy}m, provider: ${location.provider})")
        }
    }

    override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {
        Log.d("LocationHelper", "Location provider $provider status changed: $status")
    }

    override fun onProviderEnabled(provider: String) {
        Log.d("LocationHelper", "Location provider enabled: $provider")
    }

    override fun onProviderDisabled(provider: String) {
        Log.d("LocationHelper", "Location provider disabled: $provider")
    }

    private fun isBetterLocation(location: Location, currentBestLocation: Location?): Boolean {
        if (currentBestLocation == null) {
            return true
        }

        // Check whether the new location fix is newer or older
        val timeDelta = location.time - currentBestLocation.time
        val isSignificantlyNewer = timeDelta > 2 * 60 * 1000 // 2 minutes
        val isSignificantlyOlder = timeDelta < -2 * 60 * 1000
        val isNewer = timeDelta > 0

        if (isSignificantlyNewer) {
            return true
        } else if (isSignificantlyOlder) {
            return false
        }

        // Check whether the new location fix is more or less accurate
        val accuracyDelta = (location.accuracy - currentBestLocation.accuracy).toInt()
        val isLessAccurate = accuracyDelta > 0
        val isMoreAccurate = accuracyDelta < 0
        val isSignificantlyLessAccurate = accuracyDelta > 200

        // Determine location quality using a combination of timeliness and accuracy
        return when {
            isMoreAccurate -> true
            isNewer && !isLessAccurate -> true
            isNewer && !isSignificantlyLessAccurate -> true
            else -> false
        }
    }

    fun getAddressFromLocation(lat: Double, lng: Double, callback: (String?) -> Unit) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val geocoder = Geocoder(context, Locale.getDefault())
                val addresses = geocoder.getFromLocation(lat, lng, 1)
                val address = addresses?.firstOrNull()?.getAddressLine(0)

                withContext(Dispatchers.Main) {
                    callback(address)
                }
            } catch (e: Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    callback(null)
                }
            }
        }
    }

    fun hasLocationPermission(): Boolean {
        return ActivityCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }

    @SuppressLint("MissingPermission")
    fun getCurrentLocation(callback: (Location?) -> Unit) {
        if (!hasLocationPermission()) {
            callback(null)
            return
        }

        // Return current location if we have one
        if (currentLocation != null) {
            callback(currentLocation)
            return
        }

        // Try to get a fresh location
        fusedLocationClient.lastLocation
            .addOnSuccessListener { location ->
                if (location != null) {
                    currentLocation = location
                    callback(location)
                } else {
                    callback(null)
                }
            }
            .addOnFailureListener {
                callback(null)
            }
    }

    @SuppressLint("MissingPermission")
    fun getFreshLocation(callback: (Location?) -> Unit) {
        if (!hasLocationPermission()) {
            callback(null)
            return
        }

        val locationRequest = LocationRequest.create().apply {
            priority = Priority.PRIORITY_HIGH_ACCURACY
            interval = 0
            fastestInterval = 0
            numUpdates = 1
        }

        fusedLocationClient.requestLocationUpdates(
            locationRequest,
            object : LocationCallback() {
                override fun onLocationResult(result: LocationResult) {
                    fusedLocationClient.removeLocationUpdates(this)
                    val location = result.lastLocation
                    if (location != null) {
                        currentLocation = location
                    }
                    callback(location)
                }
            },
            Looper.getMainLooper()
        )
    }

    @SuppressLint("MissingPermission")
    suspend fun fetchLastKnownLocation(): Location? {
        return suspendCoroutine { cont ->
            if (!hasLocationPermission()) {
                cont.resume(null)
                return@suspendCoroutine
            }

            if (currentLocation != null) {
                cont.resume(currentLocation)
                return@suspendCoroutine
            }

            fusedLocationClient.lastLocation
                .addOnSuccessListener { location ->
                    if (location != null) {
                        currentLocation = location
                    }
                    cont.resume(location)
                }
                .addOnFailureListener { cont.resume(null) }
        }
    }

    fun isLocationAvailable(): Boolean {
        return currentLocation != null
    }

    fun getLocationInfo(): String {
        return when {
            currentLocation == null -> "Status: No location available"
            currentLocation!!.accuracy > 100f -> "Status: low accuracy (${currentLocation!!.accuracy.toInt()}m)"
            currentLocation!!.accuracy > 50f -> "Status: medium accuracy (${currentLocation!!.accuracy.toInt()}m)"
            else -> "Status: good accuracy (${currentLocation!!.accuracy.toInt()}m)"
        }
    }

    // Debug method to get detailed location status
    fun getDetailedLocationInfo(): String {
        val loc = currentLocation
        return if (loc != null) {
            val ageSeconds = (System.currentTimeMillis() - loc.time) / 1000
            "GPS: ${loc.accuracy.toInt()}m accuracy, ${ageSeconds}s old, provider: ${loc.provider}"
        } else {
            "GPS: No location available"
        }
    }
}