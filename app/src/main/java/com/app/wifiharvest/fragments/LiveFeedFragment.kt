package com.app.wifiharvest.fragments

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
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
import java.io.BufferedReader
import java.io.InputStreamReader

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

    // File picker for CSV import
    private val csvPickerLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.data?.let { uri ->
                loadCsvFile(uri)
            }
        }
    }

    // File saver for CSV export
    private val csvSaverLauncher = registerForActivityResult(
        ActivityResultContracts.CreateDocument("text/csv")
    ) { uri ->
        if (uri != null) {
            val networks = viewModel.networks.value
            if (!networks.isNullOrEmpty()) {
                saveToUserSelectedLocation(uri, networks)
            }
        }
    }

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
            (requireActivity() as MainActivity).stopScanService()  // ← Make sure this line looks like this
            stopAutoSave()

            // Visual feedback
            binding.startScanButton.isEnabled = true
            binding.stopScanButton.isEnabled = false
            binding.startScanButton.text = "Start Scanning"
            Toast.makeText(context, "Wi-Fi scanning stopped", Toast.LENGTH_SHORT).show()
        }

        binding.exportButton.setOnClickListener {
            Log.d("LiveFeedFragment", "Export button clicked")
            showExportOptions()
        }

        // Restore CSV loading functionality with debug option
        binding.btnLoadCsv.text = "Load CSV / Debug"
        binding.btnLoadCsv.setOnClickListener {
            showLoadCsvOrDebugOptions()
        }

        // Initial button states
        binding.stopScanButton.isEnabled = false
        binding.startScanButton.text = "Start Scanning"
        Log.d("LiveFeedFragment", "Buttons setup complete")
    }

    private fun showLoadCsvOrDebugOptions() {
        val options = arrayOf("Load CSV File", "Debug Data Flow", "Add Test Network")

        AlertDialog.Builder(requireContext())
            .setTitle("Choose Action")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> openCsvFilePicker()
                    1 -> debugDataFlow()
                    2 -> addTestNetwork()
                }
            }
            .show()
    }

    private fun openCsvFilePicker() {
        val intent = Intent(Intent.ACTION_GET_CONTENT).apply {
            type = "text/*"
            addCategory(Intent.CATEGORY_OPENABLE)
            putExtra(Intent.EXTRA_MIME_TYPES, arrayOf("text/csv", "text/comma-separated-values"))
        }
        csvPickerLauncher.launch(Intent.createChooser(intent, "Select CSV File"))
    }

    private fun loadCsvFile(uri: Uri) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val inputStream = requireContext().contentResolver.openInputStream(uri)
                val reader = BufferedReader(InputStreamReader(inputStream))

                var lineCount = 0
                var importedCount = 0
                var line: String?

                // Read and check header line
                val headerLine = reader.readLine()
                val hasSignalColumn = headerLine?.contains("Signal", ignoreCase = true) == true

                Log.d("LiveFeedFragment", "CSV Header: $headerLine")
                Log.d("LiveFeedFragment", "Has Signal column: $hasSignalColumn")

                while (reader.readLine().also { line = it } != null) {
                    lineCount++
                    line?.let { csvLine ->
                        val parts = csvLine.split(",").map { it.trim().removeSurrounding("\"") }

                        // Support both old format (4-5 columns) and new format (6 columns with signal)
                        val minColumns = if (hasSignalColumn) 5 else 4

                        if (parts.size >= minColumns) {
                            try {
                                val network = if (hasSignalColumn && parts.size >= 5) {
                                    // New format: SSID, BSSID, Latitude, Longitude, Signal, Physical address
                                    WifiNetwork(
                                        ssid = parts[0],
                                        bssid = parts[1],
                                        signal = parts[4].toIntOrNull() ?: -50, // Parse signal or default to -50
                                        lat = parts[2].toDouble(),
                                        lng = parts[3].toDouble(),
                                        timestamp = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date()),
                                        location = if (parts.size > 5) parts[5] else "Imported from CSV"
                                    )
                                } else {
                                    // Old format: SSID, BSSID, Latitude, Longitude, Physical address
                                    WifiNetwork(
                                        ssid = parts[0],
                                        bssid = parts[1],
                                        signal = -50, // Default signal for old format
                                        lat = parts[2].toDouble(),
                                        lng = parts[3].toDouble(),
                                        timestamp = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date()),
                                        location = if (parts.size > 4) parts[4] else "Imported from CSV"
                                    )
                                }

                                withContext(Dispatchers.Main) {
                                    viewModel.addNetwork(network)
                                }
                                importedCount++

                                Log.d("LiveFeedFragment", "Imported: ${network.ssid} (${network.signal} dBm)")

                            } catch (e: NumberFormatException) {
                                Log.w("LiveFeedFragment", "Invalid data in line $lineCount: $csvLine - ${e.message}")
                            }
                        } else {
                            Log.w("LiveFeedFragment", "Invalid CSV format in line $lineCount (${parts.size} columns): $csvLine")
                        }
                    }
                }

                reader.close()

                withContext(Dispatchers.Main) {
                    Toast.makeText(
                        requireContext(),
                        "Imported $importedCount networks from CSV (Format: ${if (hasSignalColumn) "New with Signal" else "Legacy"})",
                        Toast.LENGTH_LONG
                    ).show()
                    Log.d("LiveFeedFragment", "CSV import completed: $importedCount/$lineCount networks imported")
                }

            } catch (e: Exception) {
                Log.e("LiveFeedFragment", "Error loading CSV file", e)
                withContext(Dispatchers.Main) {
                    Toast.makeText(
                        requireContext(),
                        "Error loading CSV: ${e.message}",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
    }

    private fun showExportOptions() {
        val networks = viewModel.networks.value
        if (networks.isNullOrEmpty()) {
            Toast.makeText(context, "No networks to export", Toast.LENGTH_SHORT).show()
            return
        }

        val options = arrayOf("Save to Device", "Share")

        AlertDialog.Builder(requireContext())
            .setTitle("Export ${networks.size} Networks")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> saveToDevice(networks)
                    1 -> shareNetworks(networks)
                }
            }
            .show()
    }

    private fun saveToDevice(networks: List<WifiNetwork>) {
        // Generate filename with timestamp
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val fileName = "wifiharvest_export_$timestamp.csv"

        // Launch the system save dialog
        csvSaverLauncher.launch(fileName)
    }

    private fun saveToUserSelectedLocation(uri: Uri, networks: List<WifiNetwork>) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                // Create CSV content
                val csvContent = generateCsvContent(networks)

                // Write to user-selected location
                requireContext().contentResolver.openOutputStream(uri)?.use { outputStream ->
                    outputStream.write(csvContent.toByteArray())
                }

                withContext(Dispatchers.Main) {
                    Toast.makeText(
                        requireContext(),
                        "Successfully saved ${networks.size} networks to selected location",
                        Toast.LENGTH_LONG
                    ).show()
                    Log.d("LiveFeedFragment", "Saved ${networks.size} networks to user-selected location")
                }

            } catch (e: Exception) {
                Log.e("LiveFeedFragment", "Error saving to user-selected location", e)
                withContext(Dispatchers.Main) {
                    Toast.makeText(
                        requireContext(),
                        "Error saving file: ${e.message}",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
    }

    private fun shareNetworks(networks: List<WifiNetwork>) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                // Create CSV content
                val csvContent = generateCsvContent(networks)
                val fileName = "wifiharvest_export_${System.currentTimeMillis()}.csv"
                val file = File(requireContext().cacheDir, fileName)

                file.writeText(csvContent)

                withContext(Dispatchers.Main) {
                    // Create share intent
                    val uri = androidx.core.content.FileProvider.getUriForFile(
                        requireContext(),
                        "${requireContext().packageName}.provider",
                        file
                    )

                    val shareIntent = Intent(Intent.ACTION_SEND).apply {
                        type = "text/csv"
                        putExtra(Intent.EXTRA_STREAM, uri)
                        putExtra(Intent.EXTRA_SUBJECT, "WiFiHarvest Export - ${networks.size} Networks")
                        putExtra(Intent.EXTRA_TEXT, "WiFi networks exported from WiFiHarvest app")
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    }

                    startActivity(Intent.createChooser(shareIntent, "Share Networks"))
                }

            } catch (e: Exception) {
                Log.e("LiveFeedFragment", "Error sharing networks", e)
                withContext(Dispatchers.Main) {
                    Toast.makeText(
                        requireContext(),
                        "Error sharing: ${e.message}",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
    }

    private fun generateCsvContent(networks: List<WifiNetwork>): String {
        val csvBuilder = StringBuilder()

        // Updated header to include Signal
        csvBuilder.appendLine("SSID,BSSID,Latitude,Longitude,Signal,Physical address")

        // Data rows
        networks.forEach { network ->
            val ssid = network.ssid.replace(",", ";") // Replace commas to avoid CSV issues
            val bssid = network.bssid
            val lat = network.lat
            val lng = network.lng
            val signal = network.signal
            val address = (network.location ?: "Unknown").replace(",", ";")

            csvBuilder.appendLine("\"$ssid\",\"$bssid\",$lat,$lng,$signal,\"$address\"")
        }

        return csvBuilder.toString()
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

        Toast.makeText(context, "Debug info logged - check logcat", Toast.LENGTH_LONG).show()
    }

    private fun addTestNetwork() {
        val testNetwork = WifiNetwork(
            ssid = "TEST-${System.currentTimeMillis()}",
            bssid = "TE:ST:00:00:00:${(0..99).random().toString().padStart(2, '0')}",
            signal = -50,
            lat = 40.3872 + (Math.random() - 0.5) * 0.001, // Slight variation
            lng = -80.0452 + (Math.random() - 0.5) * 0.001,
            timestamp = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date()),
            location = "Test Location"
        )

        Log.d("LiveFeedFragment", "Adding test network: ${testNetwork.ssid}")
        viewModel.addNetwork(testNetwork)
        Toast.makeText(context, "Added test network: ${testNetwork.ssid}", Toast.LENGTH_SHORT).show()
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