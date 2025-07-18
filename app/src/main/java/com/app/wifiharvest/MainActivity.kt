package com.app.wifiharvest

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.app.wifiharvest.WifiLogAdapter
import com.app.wifiharvest.WifiScanner

private val LOCATION_PERMISSION_REQUEST_CODE = 1001

class MainActivity : AppCompatActivity() {

    private lateinit var startButton: Button
    private lateinit var stopButton: Button
    private lateinit var recyclerView: RecyclerView
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
                Toast.makeText(this, "Location permission is required for scanning.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        startButton = findViewById(R.id.startButton)
        stopButton = findViewById(R.id.stopButton)
        recyclerView = findViewById(R.id.recyclerView)

        adapter = WifiLogAdapter()
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = adapter
        locationHelper = LocationHelper(this)
        wifiScanner = WifiScanner(this, adapter, locationHelper)  // ✅ this comes before scan call

        startButton.setOnClickListener {
            if (locationHelper.hasLocationPermission()) {
                wifiScanner.startScanning()  // ✅ this is valid
            } else {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                    LOCATION_PERMISSION_REQUEST_CODE
                )
            }
        }

        stopButton.setOnClickListener {
            wifiScanner.stopScanning()
        }
    }
}