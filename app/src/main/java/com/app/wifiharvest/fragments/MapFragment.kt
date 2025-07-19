package com.app.wifiharvest.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.app.wifiharvest.R
import com.app.wifiharvest.SharedWifiViewModel
import com.app.wifiharvest.databinding.FragmentMapBinding
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.app.wifiharvest.WifiScanListener
import com.google.android.gms.location.LocationServices


class MapFragment : Fragment(), OnMapReadyCallback, WifiScanListener {

    private var _binding: FragmentMapBinding? = null
    private val binding get() = _binding!!
    private lateinit var viewModel: SharedWifiViewModel
    private lateinit var map: GoogleMap

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentMapBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        viewModel = ViewModelProvider(requireActivity())[SharedWifiViewModel::class.java]
        val mapFragment = childFragmentManager.findFragmentById(R.id.mapView) as SupportMapFragment
        mapFragment.getMapAsync(this)
    }

    override fun onMapReady(googleMap: GoogleMap) {
        map = googleMap

        LocationServices.getFusedLocationProviderClient(requireContext())
            .lastLocation
            .addOnSuccessListener { location ->
                location?.let {
                    val myLatLng = LatLng(it.latitude, it.longitude)
                    map.moveCamera(CameraUpdateFactory.newLatLngZoom(myLatLng, 16f))
                    map.addMarker(
                        MarkerOptions()
                            .position(myLatLng)
                            .title("You are here")
                    )
                }
            }

        viewModel.networks.observe(viewLifecycleOwner) { list ->
            map.clear()

            // Drop all markers
            for (network in list) {
                val latLng = LatLng(network.latitude, network.longitude)
                map.addMarker(MarkerOptions().position(latLng).title(network.ssid))
            }

            // Optional: Focus camera on the most recent scan
            list.lastOrNull()?.let {
                val focus = LatLng(it.latitude, it.longitude)
                map.moveCamera(CameraUpdateFactory.newLatLngZoom(focus, 16f))
            }
        }
    }

    override fun onNewScanResult(ssid: String, bssid: String, lat: Double, lon: Double) {
        if (::map.isInitialized) {
            val location = LatLng(lat, lon)
            map.addMarker(
                MarkerOptions()
                    .position(location)
                    .title("$ssid ($bssid)")
            )
        }
    }


    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}