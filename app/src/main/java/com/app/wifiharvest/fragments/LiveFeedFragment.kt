package com.app.wifiharvest.fragments

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
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
import com.app.wifiharvest.MainActivity
import java.io.File
import androidx.core.content.FileProvider

class LiveFeedFragment : Fragment() {

    private var _binding: FragmentLiveFeedBinding? = null
    private val binding get() = _binding!!
    private lateinit var adapter: WifiLogAdapter
    private lateinit var viewModel: SharedWifiViewModel
    private lateinit var wifiScanner: WifiScanner
    private val createFileRequestCode = 101

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

        wifiScanner = WifiScanner(requireContext(), adapter, LocationHelper(requireContext()), viewModel)

        binding.startButton.setOnClickListener {
            (activity as? MainActivity)?.startWifiScan()
        }

        binding.stopButton.setOnClickListener {
            (activity as? MainActivity)?.stopWifiScan()
        }

        binding.exportButton.setOnClickListener {
            AlertDialog.Builder(requireContext())
                .setTitle("Export Options")
                .setItems(arrayOf("Share", "Save to Device")) { _, which ->
                    when (which) {
                        0 -> shareNetworks()
                        1 -> launchSaveDialog()
                    }
                }
                .show()
        }

        // ✅ FIXED — Proper address resolution with async reverse geocoding
        viewModel.networks.observe(viewLifecycleOwner) { list ->
            list.forEach {
                LocationHelper(requireContext()).getAddressFromLocation(it.latitude, it.longitude) { address ->
                    val displayLocation = address ?: "Lat: ${it.latitude}, Lng: ${it.longitude}"

                    adapter.addLog(
                        WifiLogEntry(
                            ssid = it.ssid,
                            bssid = it.bssid,
                            timestamp = getFormattedTime(),
                            location = displayLocation
                        )
                    )
                    binding.recyclerView.scrollToPosition(adapter.itemCount - 1)
                }
            }
        }
    }

    private fun getFormattedTime(): String {
        val format = java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.getDefault())
        return format.format(java.util.Date())
    }

    private fun shareNetworks() {
        val networks = viewModel.networks.value ?: emptyList()
        val csvData = buildCsvFromNetworks(networks)
        val uri = getTempCsvUri(csvData)

        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/csv"
            putExtra(Intent.EXTRA_SUBJECT, "Wi-Fi Scan Results")
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        startActivity(Intent.createChooser(intent, "Share Wi-Fi CSV"))
    }

    private fun launchSaveDialog() {
        val intent = Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "text/csv"
            putExtra(Intent.EXTRA_TITLE, "WiFiNetworks.csv")
        }
        startActivityForResult(intent, createFileRequestCode)
    }

    private fun buildCsvFromNetworks(networks: List<WifiNetwork>): String {
        val builder = StringBuilder("SSID,BSSID,Latitude,Longitude,Address\n")
        for (net in networks) {
            val address = net.address ?: "Lat: ${net.latitude}, Lng: ${net.longitude}"
            builder.append("${net.ssid},${net.bssid},${net.latitude},${net.longitude},\"$address\"\n")
        }
        return builder.toString()
    }


    private fun getTempCsvUri(csvData: String): Uri {
        val fileName = "wifi_log_temp.csv"
        val file = File(requireContext().cacheDir, fileName)
        file.writeText(csvData)
        return FileProvider.getUriForFile(requireContext(), "${requireContext().packageName}.provider", file)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == createFileRequestCode && resultCode == Activity.RESULT_OK) {
            data?.data?.let { uri ->
                val networks = viewModel.networks.value ?: emptyList()
                try {
                    requireContext().contentResolver.openOutputStream(uri)?.bufferedWriter()?.use { writer ->
                        writer.write("SSID,BSSID,Latitude,Longitude,Address\n")
                        for (net in networks) {
                            val address = net.address ?: "Lat: ${net.latitude}, Lng: ${net.longitude}"
                            writer.write("${net.ssid},${net.bssid},${net.latitude},${net.longitude},\"$address\"\n")
                        }
                    }
                    Toast.makeText(requireContext(), "Saved to device", Toast.LENGTH_SHORT).show()
                } catch (e: Exception) {
                    Toast.makeText(requireContext(), "Failed to save: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
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
    }
}
