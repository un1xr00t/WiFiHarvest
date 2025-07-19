package com.app.wifiharvest.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.app.wifiharvest.databinding.FragmentLiveFeedBinding
import com.app.wifiharvest.WifiLogAdapter
import com.app.wifiharvest.WifiLogEntry
import com.app.wifiharvest.SharedWifiViewModel
import com.app.wifiharvest.WifiScanner
import com.app.wifiharvest.LocationHelper
import com.app.wifiharvest.WifiNetwork

class LiveFeedFragment : Fragment() {

    private var _binding: FragmentLiveFeedBinding? = null
    private val binding get() = _binding!!
    private lateinit var adapter: WifiLogAdapter
    private lateinit var viewModel: SharedWifiViewModel
    private lateinit var wifiScanner: WifiScanner

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentLiveFeedBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        viewModel = ViewModelProvider(requireActivity()).get(SharedWifiViewModel::class.java)

        adapter = WifiLogAdapter(mutableListOf())
        binding.recyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerView.adapter = adapter

        // ✅ Assign to the class-level wifiScanner
        wifiScanner = WifiScanner(requireContext(), adapter, LocationHelper(requireContext()), viewModel)

        binding.startButton.setOnClickListener {
            wifiScanner.startScanning()
        }

        binding.stopButton.setOnClickListener {
            wifiScanner.stopScanning()
        }

        viewModel.networks.observe(viewLifecycleOwner) { list ->
            val logList = list.map {
                WifiLogEntry(
                    ssid = it.ssid,
                    bssid = it.bssid,
                    timestamp = java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.getDefault()).format(java.util.Date()),
                    location = "Lat: ${it.latitude}, Lng: ${it.longitude}"
                )
            }
            adapter.updateData(logList)
        }
    }

    override fun onResume() {
        super.onResume()
        wifiScanner.setScanListener { ssid, bssid, lat, lon ->
            val newNetwork = WifiNetwork(ssid, bssid, lat, lon)
            viewModel.addNetwork(newNetwork)
        }
        wifiScanner.pushLastScanToListener()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
        wifiScanner.stopScanning() // ✅ Clean up when view is gone
    }
}
