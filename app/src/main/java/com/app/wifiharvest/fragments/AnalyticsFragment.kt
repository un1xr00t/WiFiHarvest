package com.app.wifiharvest.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.app.wifiharvest.SharedWifiViewModel
import com.app.wifiharvest.databinding.FragmentAnalyticsBinding
import com.app.wifiharvest.adapters.SSIDCountAdapter


class AnalyticsFragment : Fragment() {

    private var _binding: FragmentAnalyticsBinding? = null
    private val binding get() = _binding!!
    private lateinit var viewModel: SharedWifiViewModel
    private lateinit var adapter: SSIDCountAdapter

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentAnalyticsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        viewModel = ViewModelProvider(requireActivity())[SharedWifiViewModel::class.java]

        adapter = SSIDCountAdapter()
        binding.ssidList.layoutManager = LinearLayoutManager(requireContext())
        binding.ssidList.adapter = adapter

        viewModel.networks.observe(viewLifecycleOwner) { list ->
            val uniqueBssids = list.map { it.bssid }.toSet()
            binding.uniqueCountText.text = "Total Unique BSSIDs: ${uniqueBssids.size}"

            val ssidCounts = list.groupingBy { it.ssid }.eachCount()
                .toList()
                .sortedByDescending { it.second }

            adapter.submitList(ssidCounts)

            val signalBuckets = mutableMapOf<String, Int>()
            list.mapNotNull { it.signal }.forEach { signal ->
                val bucket = when (signal) {
                    in -30..-40 -> "-30 to -40 dBm"
                    in -41..-50 -> "-41 to -50 dBm"
                    in -51..-60 -> "-51 to -60 dBm"
                    in -61..-70 -> "-61 to -70 dBm"
                    else -> "≤ -71 dBm"
                }
                signalBuckets[bucket] = signalBuckets.getOrDefault(bucket, 0) + 1
            }

            val histogramText = signalBuckets.entries.joinToString("\n") { "${it.key}: ${it.value}" }
            binding.signalBuckets.text = histogramText
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
