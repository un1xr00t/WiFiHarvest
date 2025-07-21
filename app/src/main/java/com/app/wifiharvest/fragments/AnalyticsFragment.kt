package com.app.wifiharvest.fragments

import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.app.wifiharvest.R
import com.app.wifiharvest.SharedWifiViewModel
import com.app.wifiharvest.WifiViewModelHolder
import com.app.wifiharvest.databinding.FragmentAnalyticsBinding
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.charts.PieChart
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.data.*
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import com.github.mikephil.charting.formatter.ValueFormatter
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.components.YAxis
import com.github.mikephil.charting.utils.ColorTemplate
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.roundToInt

class AnalyticsFragment : Fragment() {

    private var _binding: FragmentAnalyticsBinding? = null
    private val binding get() = _binding!!

    private val viewModel: SharedWifiViewModel
        get() = WifiViewModelHolder.getViewModel()

    private lateinit var signalChart: BarChart
    private lateinit var securityChart: PieChart
    private lateinit var discoveryChart: LineChart
    private lateinit var topNetworksAdapter: TopNetworksAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAnalyticsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupCharts()
        setupRecyclerView()
        observeNetworks()

        // Refresh button
        binding.refreshButton.setOnClickListener {
            refreshAnalytics()
        }
    }

    private fun setupCharts() {
        signalChart = binding.signalChart
        securityChart = binding.securityChart
        discoveryChart = binding.discoveryChart

        setupSignalChart()
        setupSecurityChart()
        setupDiscoveryChart()
    }

    private fun setupSignalChart() {
        signalChart.apply {
            description.isEnabled = false
            setTouchEnabled(true)
            setDragEnabled(true)
            setScaleEnabled(true)
            setPinchZoom(false)
            setDrawBarShadow(false)
            setDrawValueAboveBar(true)
            setMaxVisibleValueCount(60)

            // Styling
            setBackgroundColor(Color.parseColor("#1E1E1E"))

            // X-axis
            xAxis.apply {
                position = XAxis.XAxisPosition.BOTTOM
                setDrawGridLines(false)
                granularity = 1f
                textColor = Color.WHITE
                textSize = 10f
            }

            // Y-axis
            axisLeft.apply {
                setLabelCount(8, false)
                setPosition(YAxis.YAxisLabelPosition.OUTSIDE_CHART)
                spaceTop = 15f
                axisMinimum = 0f
                textColor = Color.WHITE
                textSize = 10f
            }

            axisRight.isEnabled = false
            legend.isEnabled = false
        }
    }

    private fun setupSecurityChart() {
        securityChart.apply {
            setUsePercentValues(true)
            description.isEnabled = false
            setExtraOffsets(5f, 10f, 5f, 5f)

            dragDecelerationFrictionCoef = 0.95f
            setDrawHoleEnabled(true)
            setHoleColor(Color.parseColor("#1E1E1E"))
            setTransparentCircleColor(Color.WHITE)
            setTransparentCircleAlpha(110)

            holeRadius = 45f
            transparentCircleRadius = 50f

            setDrawCenterText(true)
            centerText = "Security\nTypes"
            setCenterTextColor(Color.WHITE)
            setCenterTextSize(14f)

            rotationAngle = 0f
            isRotationEnabled = true
            isHighlightPerTapEnabled = true

            // Legend
            legend.apply {
                isEnabled = true
                textColor = Color.WHITE
                textSize = 12f
                form = com.github.mikephil.charting.components.Legend.LegendForm.CIRCLE
            }
        }
    }

    private fun setupDiscoveryChart() {
        discoveryChart.apply {
            description.isEnabled = false
            setTouchEnabled(true)
            setDragEnabled(true)
            setScaleEnabled(true)
            setPinchZoom(false)
            setBackgroundColor(Color.parseColor("#1E1E1E"))

            // X-axis
            xAxis.apply {
                position = XAxis.XAxisPosition.BOTTOM
                setDrawGridLines(true)
                gridColor = Color.parseColor("#333333")
                textColor = Color.WHITE
                textSize = 10f
            }

            // Y-axis
            axisLeft.apply {
                textColor = Color.WHITE
                setDrawGridLines(true)
                gridColor = Color.parseColor("#333333")
                axisMinimum = 0f
                textSize = 10f
            }

            axisRight.isEnabled = false
            legend.apply {
                isEnabled = true
                textColor = Color.WHITE
                textSize = 12f
            }
        }
    }

    private fun setupRecyclerView() {
        topNetworksAdapter = TopNetworksAdapter()
        binding.topNetworksRecyclerView.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = topNetworksAdapter
        }
    }

    private fun observeNetworks() {
        viewModel.networks.observe(viewLifecycleOwner) { networks ->
            Log.d("AnalyticsFragment", "Updating analytics with ${networks.size} networks")
            updateAnalytics(networks)
        }
    }

    private fun refreshAnalytics() {
        val networks = viewModel.networks.value ?: emptyList()
        updateAnalytics(networks)
        binding.refreshButton.animate()
            .rotationBy(360f)
            .setDuration(500)
            .start()
    }

    private fun updateAnalytics(networks: List<com.app.wifiharvest.WifiNetwork>) {
        updateOverviewStats(networks)
        updateSignalChart(networks)
        updateSecurityChart(networks)
        updateDiscoveryChart(networks)
        updateTopNetworks(networks)
    }

    private fun updateOverviewStats(networks: List<com.app.wifiharvest.WifiNetwork>) {
        val totalNetworks = networks.size
        val uniqueSSIDs = networks.map { it.ssid }.filter { it.isNotBlank() }.distinct().size
        val hiddenNetworks = networks.count { it.ssid.isBlank() }
        val avgSignal = if (networks.isNotEmpty()) {
            networks.map { it.signal }.average().roundToInt()
        } else 0

        val strongSignals = networks.count { it.signal >= -50 }
        val coverage = if (totalNetworks > 0) {
            ((strongSignals.toFloat() / totalNetworks) * 100).roundToInt()
        } else 0

        binding.apply {
            totalNetworksText.text = totalNetworks.toString()
            uniqueSSIDsText.text = uniqueSSIDs.toString()
            hiddenNetworksText.text = hiddenNetworks.toString()
            avgSignalText.text = "${avgSignal} dBm"
            coveragePercentText.text = "${coverage}%"

            // Update last scan time
            val lastScan = networks.maxByOrNull { it.timestamp ?: "" }?.timestamp
            lastScanText.text = lastScan ?: "No scans yet"
        }
    }

    private fun updateSignalChart(networks: List<com.app.wifiharvest.WifiNetwork>) {
        val signalRanges = mapOf(
            "Excellent\n(-30 to -50)" to networks.count { it.signal >= -50 },
            "Good\n(-51 to -65)" to networks.count { it.signal in -65..-51 },
            "Fair\n(-66 to -75)" to networks.count { it.signal in -75..-66 },
            "Poor\n(-76 to -85)" to networks.count { it.signal in -85..-76 },
            "Very Poor\n(< -85)" to networks.count { it.signal < -85 }
        )

        val entries = signalRanges.values.mapIndexed { index, count ->
            BarEntry(index.toFloat(), count.toFloat())
        }

        val dataSet = BarDataSet(entries, "Signal Strength Distribution").apply {
            colors = listOf(
                Color.parseColor("#00FF00"), // Excellent - Green
                Color.parseColor("#7FFF00"), // Good - Light Green
                Color.parseColor("#FFFF00"), // Fair - Yellow
                Color.parseColor("#FF7F00"), // Poor - Orange
                Color.parseColor("#FF0000")  // Very Poor - Red
            )
            valueTextColor = Color.WHITE
            valueTextSize = 10f
        }

        signalChart.apply {
            data = BarData(dataSet)
            xAxis.valueFormatter = IndexAxisValueFormatter(signalRanges.keys.toList())
            animateY(1000)
            invalidate()
        }
    }

    private fun updateSecurityChart(networks: List<com.app.wifiharvest.WifiNetwork>) {
        // For now, we'll simulate security types based on SSID patterns
        val securityTypes = mutableMapOf<String, Int>()

        networks.forEach { network ->
            val securityType = when {
                network.ssid.contains("_5G", ignoreCase = true) -> "WPA3 (5GHz)"
                network.ssid.contains("Guest", ignoreCase = true) -> "Open/Guest"
                network.ssid.contains("xfinity", ignoreCase = true) -> "Enterprise"
                network.ssid.isBlank() -> "Hidden"
                else -> "WPA2/WPA3"
            }
            securityTypes[securityType] = securityTypes.getOrDefault(securityType, 0) + 1
        }

        val entries = securityTypes.map { (type, count) ->
            PieEntry(count.toFloat(), type)
        }

        val dataSet = PieDataSet(entries, "Security Types").apply {
            setDrawIcons(false)
            sliceSpace = 3f
            iconsOffset = com.github.mikephil.charting.utils.MPPointF(0f, 40f)
            selectionShift = 5f

            colors = listOf(
                Color.parseColor("#FF6B6B"), // Red
                Color.parseColor("#4ECDC4"), // Teal
                Color.parseColor("#45B7D1"), // Blue
                Color.parseColor("#96CEB4"), // Green
                Color.parseColor("#FECA57"), // Yellow
                Color.parseColor("#FF9FF3"), // Pink
                Color.parseColor("#54A0FF")  // Light Blue
            )

            valueLinePart1OffsetPercentage = 80f
            valueLinePart1Length = 0.2f
            valueLinePart2Length = 0.4f
            yValuePosition = PieDataSet.ValuePosition.OUTSIDE_SLICE
        }

        dataSet.valueFormatter = object : ValueFormatter() {
            override fun getFormattedValue(value: Float): String {
                return "${value.toInt()}"
            }
        }

        securityChart.apply {
            data = PieData(dataSet).apply {
                setValueFormatter(dataSet.valueFormatter)
                setValueTextSize(11f)
                setValueTextColor(Color.WHITE)
            }
            animateY(1400, com.github.mikephil.charting.animation.Easing.EaseInOutQuad)
            invalidate()
        }
    }

    private fun updateDiscoveryChart(networks: List<com.app.wifiharvest.WifiNetwork>) {
        // Group networks by hour for discovery timeline
        val hourlyDiscovery = mutableMapOf<String, Int>()
        val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:00", Locale.getDefault())

        networks.forEach { network ->
            val timestamp = network.timestamp
            if (!timestamp.isNullOrBlank()) {
                try {
                    val date = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).parse(timestamp)
                    if (date != null) {
                        val hourKey = dateFormat.format(date)
                        hourlyDiscovery[hourKey] = hourlyDiscovery.getOrDefault(hourKey, 0) + 1
                    }
                } catch (e: Exception) {
                    // Skip invalid timestamps
                }
            }
        }

        val sortedHours = hourlyDiscovery.toList().sortedBy { it.first }.takeLast(24) // Last 24 hours

        val entries = sortedHours.mapIndexed { index, (_, count) ->
            Entry(index.toFloat(), count.toFloat())
        }

        val dataSet = LineDataSet(entries, "Networks Discovered Over Time").apply {
            color = Color.parseColor("#00FF00")
            setCircleColor(Color.parseColor("#00FF00"))
            lineWidth = 2f
            circleRadius = 4f
            setDrawCircleHole(false)
            valueTextSize = 9f
            valueTextColor = Color.WHITE
            setDrawFilled(true)
            fillColor = Color.parseColor("#00FF00")
            fillAlpha = 50
        }

        discoveryChart.apply {
            data = LineData(dataSet)
            xAxis.valueFormatter = IndexAxisValueFormatter(
                sortedHours.map { it.first.substring(11, 16) } // Show only HH:MM
            )
            animateX(1000)
            invalidate()
        }
    }

    private fun updateTopNetworks(networks: List<com.app.wifiharvest.WifiNetwork>) {
        // Get top networks by signal strength
        val topNetworks = networks
            .filter { it.ssid.isNotBlank() }
            .sortedByDescending { it.signal }
            .take(10)

        topNetworksAdapter.submitList(topNetworks)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}