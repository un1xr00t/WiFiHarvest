package com.app.wifiharvest

import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.findNavController
import androidx.navigation.ui.NavigationUI
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.app.wifiharvest.fragments.MapFragment
import androidx.lifecycle.ViewModelProvider
import com.app.wifiharvest.MainActivity

private val LOCATION_PERMISSION_REQUEST_CODE = 1001

class MainActivity : AppCompatActivity() {

    private lateinit var adapter: WifiLogAdapter
    private lateinit var wifiScanner: WifiScanner
    private lateinit var locationHelper: LocationHelper

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                wifiScanner.startScanning()
            } else {
                Toast.makeText(
                    this,
                    "Location permission is required for scanning.",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        adapter = WifiLogAdapter(mutableListOf<WifiLogEntry>())
        locationHelper = LocationHelper(this)
        val viewModel = ViewModelProvider(this)[SharedWifiViewModel::class.java] // ✅ add this
        wifiScanner = WifiScanner(this, adapter, locationHelper, viewModel) // ✅ fix this

        val navController = findNavController(R.id.fragment_container)
        val bottomNav = findViewById<BottomNavigationView>(R.id.bottom_nav)
        NavigationUI.setupWithNavController(bottomNav, navController)

        navController.addOnDestinationChangedListener { _, destination, _ ->
            when (destination.id) {
                R.id.nav_map -> {
                    val mapFragment = supportFragmentManager
                        .primaryNavigationFragment
                        ?.childFragmentManager
                        ?.fragments
                        ?.firstOrNull { it is MapFragment }

                    if (mapFragment is WifiScanListener) {
                        wifiScanner.setScanListener(mapFragment)
                        wifiScanner.pushLastScanToListener() // ✅ immediately show last scan
                    } else {
                        wifiScanner.setScanListener(null)
                    }
                }

                else -> wifiScanner.setScanListener(null)
            }
        }

        bottomNav.selectedItemId = R.id.nav_feed
    }

    fun startWifiScan() {
        wifiScanner.startScanning()
    }

    fun stopWifiScan() {
        wifiScanner.stopScanning()
    }
}

