package com.app.wifiharvest.fragments

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.app.wifiharvest.*
import com.app.wifiharvest.databinding.FragmentLiveFeedBinding

class LiveFeedFragment : Fragment() {

    private var _binding: FragmentLiveFeedBinding? = null
    private val binding get() = _binding!!

    private lateinit var wifiScanner: WifiScanner
    private lateinit var locationHelper: LocationHelper
    private lateinit var adapter: WifiLogAdapter

    private val LOCATION_PERMISSION_REQUEST_CODE = 1001

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentLiveFeedBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        adapter = WifiLogAdapter()
        binding.recyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerView.adapter = adapter

        locationHelper = LocationHelper(requireContext())
        wifiScanner = WifiScanner(requireContext(), adapter, locationHelper)

        binding.startButton.setOnClickListener {
            if (locationHelper.hasLocationPermission()) {
                wifiScanner.startScanning()
            } else {
                ActivityCompat.requestPermissions(
                    requireActivity(),
                    arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                    LOCATION_PERMISSION_REQUEST_CODE
                )
            }
        }

        binding.stopButton.setOnClickListener {
            wifiScanner.stopScanning()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
