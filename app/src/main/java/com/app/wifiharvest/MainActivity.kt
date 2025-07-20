package com.app.wifiharvest

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import com.app.wifiharvest.databinding.ActivityMainBinding
import com.app.wifiharvest.services.WifiScanService

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var navController: NavController

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val fineGranted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] ?: false
        val coarseGranted = permissions[Manifest.permission.ACCESS_COARSE_LOCATION] ?: false
        val backgroundGranted = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            permissions[Manifest.permission.ACCESS_BACKGROUND_LOCATION] ?: false
        } else {
            true // Not needed on Android 9 and below
        }

        when {
            fineGranted && backgroundGranted -> {
                Toast.makeText(this, "All location permissions granted", Toast.LENGTH_SHORT).show()
            }
            fineGranted || coarseGranted -> {
                Toast.makeText(this, "Location permission granted, but background location is needed for continuous scanning", Toast.LENGTH_LONG).show()
            }
            else -> {
                Toast.makeText(this, "Location permission is required for Wi-Fi scanning", Toast.LENGTH_LONG).show()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val navHostFragment =
            supportFragmentManager.findFragmentById(R.id.fragment_container) as NavHostFragment
        navController = navHostFragment.navController
        binding.bottomNav.setupWithNavController(navController)

        requestLocationPermissions()
    }

    private fun requestLocationPermissions() {
        val permissionsToRequest = mutableListOf<String>()

        // Check fine location permission
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
            != PackageManager.PERMISSION_GRANTED) {
            permissionsToRequest.add(Manifest.permission.ACCESS_FINE_LOCATION)
        }

        // Check coarse location permission
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)
            != PackageManager.PERMISSION_GRANTED) {
            permissionsToRequest.add(Manifest.permission.ACCESS_COARSE_LOCATION)
        }

        // Check background location permission (Android 10+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_BACKGROUND_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
                permissionsToRequest.add(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
            }
        }

        if (permissionsToRequest.isNotEmpty()) {
            permissionLauncher.launch(permissionsToRequest.toTypedArray())
        }
    }

    fun startScanService() {
        // Check if Wi-Fi is enabled
        val wifiManager = applicationContext.getSystemService(Context.WIFI_SERVICE) as android.net.wifi.WifiManager
        if (!wifiManager.isWifiEnabled) {
            Toast.makeText(this, "Please enable Wi-Fi for scanning", Toast.LENGTH_LONG).show()
            return
        }

        // Check if location services are enabled
        val locationManager = getSystemService(Context.LOCATION_SERVICE) as android.location.LocationManager
        val gpsEnabled = locationManager.isProviderEnabled(android.location.LocationManager.GPS_PROVIDER)
        val networkEnabled = locationManager.isProviderEnabled(android.location.LocationManager.NETWORK_PROVIDER)

        if (!gpsEnabled && !networkEnabled) {
            Toast.makeText(this, "Please enable Location Services for Wi-Fi scanning", Toast.LENGTH_LONG).show()
            return
        }

        // Check permissions
        val hasLocationPermission = ActivityCompat.checkSelfPermission(
            this, Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        if (!hasLocationPermission) {
            Toast.makeText(this, "Location permission is required", Toast.LENGTH_LONG).show()
            requestLocationPermissions()
            return
        }

        val intent = Intent(this, WifiScanService::class.java)
        ContextCompat.startForegroundService(this, intent)
        Toast.makeText(this, "Wi-Fi scanning started", Toast.LENGTH_SHORT).show()
    }

    fun stopScanService() {
        val intent = Intent(this, WifiScanService::class.java)
        stopService(intent)
        Toast.makeText(this, "Wi-Fi scanning stopped", Toast.LENGTH_SHORT).show()
    }

    private fun hasAllRequiredPermissions(): Boolean {
        val fineLocation = ActivityCompat.checkSelfPermission(
            this, Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        val backgroundLocation = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ActivityCompat.checkSelfPermission(
                this, Manifest.permission.ACCESS_BACKGROUND_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }

        return fineLocation && backgroundLocation
    }
}