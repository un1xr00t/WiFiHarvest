package com.app.wifiharvest.fragments

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.app.wifiharvest.ExportHelper
import com.app.wifiharvest.WifiLogAdapter
import com.app.wifiharvest.WifiNetwork
import com.app.wifiharvest.databinding.FragmentLiveFeedBinding
import com.app.wifiharvest.SharedWifiViewModel
import com.app.wifiharvest.models.WifiEntry
import com.app.wifiharvest.MainActivity
import kotlinx.coroutines.*
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import com.app.wifiharvest.WifiViewModelHolder

class LiveFeedFragment : Fragment() {

    private var _binding: FragmentLiveFeedBinding? = null
    private val binding get() = _binding!!

    private lateinit var adapter: WifiLogAdapter
    // Use singleton ViewModel to ensure same instance across app
    private val viewModel: SharedWifiViewModel
        get() {
            val vm = WifiViewModelHolder.getViewModel()
            Log.d("LiveFeedFragment", "Using ViewModel instance: ${vm.hashCode()}")
            return vm
        }

    // Auto-save functionality
    private val autoSaveHandler = Handler(Looper.getMainLooper())
    private val autoSaveInterval = 30000L // 30 seconds
    private var isAutoSaving = false

    private val autoSaveRunnable = object : Runnable {
        override fun run() {
            if (isAutoSaving) {
                autoSaveToJson()
                autoSaveHandler.postDelayed(this, autoSaveInterval)
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentLiveFeedBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        setupRecyclerView()
        setupButtons()
        observeNetworks()

        // Start auto-save when fragment is created
        startAutoSave()
    }

    private fun setupRecyclerView() {
        adapter = WifiLogAdapter()
        binding.recyclerView.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = this@LiveFeedFragment.adapter
        }
    }

    private fun setupButtons() {
        binding.startScanButton.setOnClickListener {
            Log.d("LiveFeedFragment", "Start scan button clicked")
            (requireActivity() as MainActivity).startScanService()
            startAutoSave()

            // Visual feedback
            binding.startScanButton.isEnabled = false
            binding.stopScanButton.isEnabled = true
            binding.startScanButton.text = "Scanning..."
            Toast.makeText(context, "Wi-Fi scanning started", Toast.LENGTH_SHORT).show()
        }

        binding.stopScanButton.setOnClickListener {
            Log.d("LiveFeedFragment", "Stop scan button clicked")
            (requireActivity() as MainActivity).stopScanService()
            stopAutoSave()

            // Visual feedback
            binding.startScanButton.isEnabled = true
            binding.stopScanButton.isEnabled = false
            binding.startScanButton.text = "Start Scanning"
            Toast.makeText(context, "Wi-Fi scanning stopped", Toast.LENGTH_SHORT).show()
        }

        binding.exportButton.setOnClickListener {
            Log.d("LiveFeedFragment", "Export button clicked")
            exportCurrentData()
        }

        // Debug button with multiple functions
        binding.btnLoadCsv.text = "Debug & Test"
        binding.btnLoadCsv.setOnClickListener {
            debugDataFlow()
        }

        // Initial button states
        binding.stopScanButton.isEnabled = false
        binding.startScanButton.text = "Start Scanning"
        Log.d("LiveFeedFragment", "Buttons setup complete")
    }

    private fun debugDataFlow() {
        Log.d("LiveFeedFragment", "=== DEBUG DATA FLOW ===")

        // 1. Check ViewModel instance
        val fragmentViewModel = viewModel
        Log.d("LiveFeedFragment", "Fragment ViewModel: ${fragmentViewModel.hashCode()}")

        val singletonViewModel = WifiViewModelHolder.getViewModel()
        Log.d("LiveFeedFragment", "Singleton ViewModel: ${singletonViewModel.hashCode()}")

        val sameInstance = fragmentViewModel === singletonViewModel
        Log.d("LiveFeedFragment", "Same ViewModel instance? $sameInstance")

        // 2. Check current networks
        val networks = fragmentViewModel.networks.value
        Log.d("LiveFeedFragment", "Networks in ViewModel: ${networks?.size ?: 0}")

        networks?.forEachIndexed { index, network ->
            Log.d("LiveFeedFragment", "  [$index] ${network.ssid} (${network.bssid}) at ${network.lat}, ${network.lng}")
        }

        // 3. Check adapter
        Log.d("LiveFeedFragment", "Adapter item count: ${adapter.itemCount}")

        // 4. Add test network to verify UI updates
        val testNetwork = WifiNetwork(
            ssid = "DEBUG-${System.currentTimeMillis()}",
            bssid = "DE:BU:G0:00:00:${(0..99).random().toString().padStart(2, '0')}",
            signal = -50,
            lat = 40.3872 + (Math.random() - 0.5) * 0.001, // Slight variation
            lng = -80.0452 + (Math.random() - 0.5) * 0.001,
            timestamp = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date()),
            location = "Debug Location"
        )

        Log.d("LiveFeedFragment", "Adding debug network: ${testNetwork.ssid}")
        fragmentViewModel.addNetwork(testNetwork)

        Toast.makeText(context, "Debug info logged - check logcat", Toast.LENGTH_LONG).show()
    }

    private fun observeNetworks() {
        viewModel.networks.observe(viewLifecycleOwner) { networks ->
            Log.d("LiveFeedFragment", "Received ${networks.size} networks from ViewModel")

            val wifiEntries = networks.map { network ->
                Log.d("LiveFeedFragment", "Network: ${network.ssid} (${network.bssid}) at ${network.lat}, ${network.lng}")
                WifiEntry(
                    SSID = network.ssid,
                    BSSID = network.bssid,
                    lat = network.lat,
                    lng = network.lng,
                    signal = network.signal,
                    timestamp = network.timestamp,
                    location = network.location
                )
            }

            // Update adapter with new data
            adapter.submitList(wifiEntries) {
                // Auto-scroll to bottom when new items are added
                if (wifiEntries.isNotEmpty()) {
                    Log.d("LiveFeedFragment", "Auto-scrolling to position ${wifiEntries.size - 1}")
                    binding.recyclerView.scrollToPosition(wifiEntries.size - 1)
                }
            }

            // Update export button text with count
            binding.exportButton.text = "Export (${networks.size})"

            // Show/hide empty state
            if (networks.isEmpty()) {
                binding.recyclerView.visibility = View.GONE
                Log.d("LiveFeedFragment", "No networks - hiding RecyclerView")
            } else {
                binding.recyclerView.visibility = View.VISIBLE
                Log.d("LiveFeedFragment", "Showing RecyclerView with ${networks.size} networks")
            }
        }

        // Optional: Push last known scan point to the map
        viewModel.pushLastNetworkToMap()
    }

    private fun exportCurrentData() {
        val networks = viewModel.networks.value
        if (networks.isNullOrEmpty()) {
            Toast.makeText(context, "No networks to export", Toast.LENGTH_SHORT).show()
            return
        }

        val csvFile = ExportHelper.exportNetworksToCSV(requireContext(), networks)
        if (csvFile != null) {
            ExportHelper.shareCSV(requireContext(), csvFile)
            Toast.makeText(context, "Exported ${networks.size} networks", Toast.LENGTH_SHORT).show()
        }
    }

    private fun startAutoSave() {
        if (!isAutoSaving) {
            isAutoSaving = true
            autoSaveHandler.post(autoSaveRunnable)
        }
    }

    private fun stopAutoSave() {
        isAutoSaving = false
        autoSaveHandler.removeCallbacks(autoSaveRunnable)
    }

    private fun autoSaveToJson() {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val networks = viewModel.networks.value
                if (networks.isNullOrEmpty()) return@launch

                val jsonArray = JSONArray()
                val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())

                networks.forEach { network ->
                    val jsonObject = JSONObject().apply {
                        put("ssid", network.ssid)
                        put("bssid", network.bssid)
                        put("signal", network.signal)
                        put("latitude", network.lat)
                        put("longitude", network.lng)
                        put("timestamp", network.timestamp ?: dateFormat.format(Date()))
                        put("location", network.location ?: "Unknown")
                    }
                    jsonArray.put(jsonObject)
                }

                val fileName = "wifiharvest_autosave_${System.currentTimeMillis()}.json"
                val file = File(requireContext().getExternalFilesDir(null), fileName)

                file.writeText(jsonArray.toString(2))

                // Keep only last 5 auto-save files
                cleanupOldAutoSaves()

            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun cleanupOldAutoSaves() {
        try {
            val saveDir = requireContext().getExternalFilesDir(null)
            val autoSaveFiles = saveDir?.listFiles { file ->
                file.name.startsWith("wifiharvest_autosave_") && file.name.endsWith(".json")
            }?.sortedByDescending { it.lastModified() }

            // Keep only the 5 most recent auto-save files
            autoSaveFiles?.drop(5)?.forEach { file ->
                file.delete()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        stopAutoSave()
        _binding = null
    }
}