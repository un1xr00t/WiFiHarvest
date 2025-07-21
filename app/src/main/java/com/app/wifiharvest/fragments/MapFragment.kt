package com.app.wifiharvest.fragments

import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.app.wifiharvest.R
import com.app.wifiharvest.SharedWifiViewModel
import com.app.wifiharvest.WifiViewModelHolder
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.*
import com.google.android.material.floatingactionbutton.FloatingActionButton

class MapFragment : Fragment(), OnMapReadyCallback {

    private lateinit var googleMap: GoogleMap
    private val viewModel: SharedWifiViewModel
        get() = WifiViewModelHolder.getViewModel()

    private val networkMarkers = mutableMapOf<String, Marker>()
    private var isMapReady = false
    private var currentMapType = GoogleMap.MAP_TYPE_NORMAL

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_map, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Get the SupportMapFragment and request notification when the map is ready
        val mapFragment = childFragmentManager.findFragmentById(R.id.map) as SupportMapFragment?
        mapFragment?.getMapAsync(this)

        // Setup FAB buttons
        setupFabButtons(view)

        // Observe network changes
        observeNetworks()
    }

    private fun setupFabButtons(view: View) {
        val fabCenter = view.findViewById<FloatingActionButton>(R.id.fabCenterLocation)
        val fabMapType = view.findViewById<FloatingActionButton>(R.id.fabMapType)

        fabCenter?.setOnClickListener {
            centerOnLatestNetwork()
        }

        fabMapType?.setOnClickListener {
            toggleMapType()
        }
    }

    private fun toggleMapType() {
        if (!isMapReady) return

        currentMapType = when (currentMapType) {
            GoogleMap.MAP_TYPE_NORMAL -> GoogleMap.MAP_TYPE_SATELLITE
            GoogleMap.MAP_TYPE_SATELLITE -> GoogleMap.MAP_TYPE_HYBRID
            GoogleMap.MAP_TYPE_HYBRID -> GoogleMap.MAP_TYPE_TERRAIN
            else -> GoogleMap.MAP_TYPE_NORMAL
        }

        googleMap.mapType = currentMapType

        val typeName = when (currentMapType) {
            GoogleMap.MAP_TYPE_NORMAL -> "Normal"
            GoogleMap.MAP_TYPE_SATELLITE -> "Satellite"
            GoogleMap.MAP_TYPE_HYBRID -> "Hybrid"
            GoogleMap.MAP_TYPE_TERRAIN -> "Terrain"
            else -> "Normal"
        }

        Log.d("MapFragment", "Changed map type to: $typeName")
    }

    override fun onMapReady(map: GoogleMap) {
        googleMap = map
        isMapReady = true

        Log.d("MapFragment", "Google Map is ready")

        // Configure map settings
        googleMap.apply {
            mapType = GoogleMap.MAP_TYPE_NORMAL
            uiSettings.isZoomControlsEnabled = true
            uiSettings.isCompassEnabled = true
            uiSettings.isMyLocationButtonEnabled = true

            // Try to enable location if permission is granted
            try {
                isMyLocationEnabled = true
            } catch (e: SecurityException) {
                Log.w("MapFragment", "Location permission not granted for map")
            }
        }

        // Set up marker click listener
        googleMap.setOnMarkerClickListener { marker ->
            marker.showInfoWindow()
            true
        }

        // Load existing networks if any
        loadExistingNetworks()

        // Default location (Pittsburgh area based on your location helper coordinates)
        val defaultLocation = LatLng(40.3872, -80.0452)
        googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(defaultLocation, 12f))
    }

    private fun observeNetworks() {
        viewModel.networks.observe(viewLifecycleOwner) { networks ->
            Log.d("MapFragment", "Received ${networks.size} networks for map display")

            if (isMapReady) {
                updateMapMarkers(networks)
            }
        }

        // Observe location updates to move camera
        viewModel.lastKnownLocation.observe(viewLifecycleOwner) { location ->
            if (isMapReady) {
                Log.d("MapFragment", "Moving camera to: ${location.latitude}, ${location.longitude}")
                googleMap.animateCamera(
                    CameraUpdateFactory.newLatLngZoom(location, 15f)
                )
            }
        }
    }

    private fun loadExistingNetworks() {
        val networks = viewModel.networks.value
        if (!networks.isNullOrEmpty()) {
            updateMapMarkers(networks)
        }
    }

    private fun updateMapMarkers(networks: List<com.app.wifiharvest.WifiNetwork>) {
        // Remove markers for networks that no longer exist
        val currentBSSIDs = networks.map { it.bssid }.toSet()
        val markersToRemove = networkMarkers.keys.filter { it !in currentBSSIDs }

        markersToRemove.forEach { bssid ->
            networkMarkers[bssid]?.remove()
            networkMarkers.remove(bssid)
        }

        // Add or update markers for each network
        networks.forEach { network ->
            addOrUpdateNetworkMarker(network)
        }

        Log.d("MapFragment", "Updated map with ${networks.size} network markers")

        // If this is the first network, move camera to it
        if (networks.isNotEmpty() && networkMarkers.size == 1) {
            val firstNetwork = networks.first()
            val location = LatLng(firstNetwork.lat, firstNetwork.lng)
            googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(location, 15f))
        }
    }

    private fun addOrUpdateNetworkMarker(network: com.app.wifiharvest.WifiNetwork) {
        val position = LatLng(network.lat, network.lng)

        // Check if marker already exists
        val existingMarker = networkMarkers[network.bssid]
        if (existingMarker != null) {
            // Update existing marker if location changed
            if (existingMarker.position != position) {
                existingMarker.position = position
            }
            // Update info window content
            existingMarker.tag = network
            return
        }

        // Create new marker
        val displaySSID = if (network.ssid.isBlank()) "(Hidden Network)" else network.ssid
        val signalColor = getSignalColor(network.signal)

        val markerOptions = MarkerOptions().apply {
            position(position)
            title(displaySSID)
            snippet(createMarkerSnippet(network))
            icon(createCustomMarkerIcon(signalColor))
        }

        val marker = googleMap.addMarker(markerOptions)
        marker?.tag = network

        if (marker != null) {
            networkMarkers[network.bssid] = marker
            Log.d("MapFragment", "Added marker for: $displaySSID at ${network.lat}, ${network.lng}")
        }
    }

    private fun createMarkerSnippet(network: com.app.wifiharvest.WifiNetwork): String {
        val parts = mutableListOf<String>()

        parts.add("BSSID: ${network.bssid}")
        parts.add("Signal: ${network.signal} dBm")

        if (!network.location.isNullOrBlank() &&
            network.location != "Loading address..." &&
            network.location != "Address unavailable") {
            parts.add("Location: ${network.location}")
        }

        parts.add("Coords: ${String.format("%.6f", network.lat)}, ${String.format("%.6f", network.lng)}")

        if (!network.timestamp.isNullOrBlank()) {
            parts.add("Time: ${network.timestamp}")
        }

        return parts.joinToString("\n")
    }

    private fun getSignalColor(signal: Int): Int {
        return when {
            signal >= -50 -> Color.parseColor("#00FF00") // Green - Excellent
            signal >= -65 -> Color.parseColor("#7FFF00") // Light Green - Good
            signal >= -75 -> Color.parseColor("#FFFF00") // Yellow - Fair
            signal >= -85 -> Color.parseColor("#FF7F00") // Orange - Poor
            else -> Color.parseColor("#FF0000") // Red - Very Poor
        }
    }

    private fun createCustomMarkerIcon(color: Int): BitmapDescriptor {
        // Create a colored circle marker
        val hsv = FloatArray(3)
        Color.colorToHSV(color, hsv)

        return when {
            hsv[0] < 60 || hsv[0] > 300 -> BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED)
            hsv[0] < 120 -> BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_YELLOW)
            hsv[0] < 180 -> BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN)
            hsv[0] < 240 -> BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_CYAN)
            hsv[0] < 300 -> BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_BLUE)
            else -> BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_MAGENTA)
        }
    }

    // Public method to center map on latest network
    fun centerOnLatestNetwork() {
        val networks = viewModel.networks.value
        val latestNetwork = networks?.lastOrNull()

        if (latestNetwork != null && isMapReady) {
            val location = LatLng(latestNetwork.lat, latestNetwork.lng)
            googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(location, 16f))

            // Show info window for the latest marker
            networkMarkers[latestNetwork.bssid]?.showInfoWindow()
        }
    }

    // Public method to clear all markers
    fun clearAllMarkers() {
        networkMarkers.values.forEach { it.remove() }
        networkMarkers.clear()
    }

    // Public method to change map type
    fun setMapType(mapType: Int) {
        if (isMapReady) {
            googleMap.mapType = mapType
        }
    }

    companion object {
        fun newInstance() = MapFragment()
    }
}