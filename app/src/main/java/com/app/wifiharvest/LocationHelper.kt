package com.app.wifiharvest

import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.Manifest
import androidx.core.app.ActivityCompat
import com.google.android.gms.location.LocationServices
import android.location.Geocoder
import java.util.Locale

class   LocationHelper(private val context: Context) {

    private val fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)

    fun getAddressFromLocation(lat: Double, lng: Double, callback: (String?) -> Unit) {
        val geocoder = Geocoder(context, Locale.getDefault())
        try {
            val addresses = geocoder.getFromLocation(lat, lng, 1)
            if (!addresses.isNullOrEmpty()) {
                val address = addresses[0].getAddressLine(0)
                callback(address)
            } else {
                callback(null)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            callback(null)
        }
    }

    fun hasLocationPermission(): Boolean {
        return ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
    }

    @SuppressLint("MissingPermission")
    fun getCurrentLocation(callback: (Location?) -> Unit) {
        if (!hasLocationPermission()) {
            callback(null)
            return
        }

        fusedLocationClient.lastLocation
            .addOnSuccessListener { callback(it) }
            .addOnFailureListener { callback(null) }
    }
}
