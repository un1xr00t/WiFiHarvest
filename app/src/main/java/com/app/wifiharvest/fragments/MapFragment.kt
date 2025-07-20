package com.app.wifiharvest.fragments

import android.location.Geocoder
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.app.wifiharvest.R
import com.app.wifiharvest.SharedWifiViewModel
import com.app.wifiharvest.databinding.FragmentMapBinding
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import java.util.Locale

class MapFragment : Fragment(), OnMapReadyCallback {

    private var _binding: FragmentMapBinding? = null
    private val binding get() = _binding!!

    private lateinit var googleMap: GoogleMap
    private val viewModel: SharedWifiViewModel by activityViewModels()
    private val shownAddresses = mutableSetOf<String>()
    private var initialZoomDone = false

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentMapBinding.inflate(inflater, container, false)

        val mapFragment =
            childFragmentManager.findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)

        return binding.root
    }

    override fun onMapReady(map: GoogleMap) {
        googleMap = map
        val geocoder = Geocoder(requireContext(), Locale.getDefault())

        viewModel.networks.observe(viewLifecycleOwner) { wifiList ->
            if (wifiList.isEmpty()) return@observe

            for (network in wifiList) {
                val latLng = LatLng(network.lat, network.lng)

                val addressLine = try {
                    geocoder.getFromLocation(network.lat, network.lng, 1)
                        ?.firstOrNull()
                        ?.getAddressLine(0)
                        ?: "${network.lat},${network.lng}"
                } catch (e: Exception) {
                    "${network.lat},${network.lng}"
                }

                if (shownAddresses.contains(addressLine)) continue

                googleMap.addMarker(
                    MarkerOptions()
                        .position(latLng)
                        .title(network.ssid.ifBlank { "(Hidden SSID)" })
                        .snippet("Address: $addressLine")
                )

                shownAddresses.add(addressLine)

                if (!initialZoomDone) {
                    googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, 18f))
                    initialZoomDone = true
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
