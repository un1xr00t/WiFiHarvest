package com.app.wifiharvest.fragments

import android.app.Activity
import android.app.AlertDialog
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
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
import android.app.ActivityManager

class LiveFeedFragment : Fragment() {

    private var _binding: FragmentLiveFeedBinding? = null
    private val binding get() = _binding!!

    private lateinit var adapter: WifiLogAdapter

    // Use singleton ViewModel to ensure same instance across app
    private val viewModel: SharedWifiViewModel
        get() {
            val vm = WifiViewModelHolder.getViewModel()
            Log.d("LiveFeedFragment", "🤖 Using ViewModel instance: ${vm.hashCode()}")
            return vm
        }

    // Terminal banner references
    private lateinit var terminalPrompt: TextView
    private lateinit var networkStats: TextView
    private lateinit var signalBars: TextView
    private lateinit var signalPercent: TextView
    private lateinit var gpsCoords: TextView
    private lateinit var scanDuration: TextView
    private lateinit var statusText: TextView
    private lateinit var networkTopology: TextView

    // Auto-save functionality
    private val autoSaveHandler = Handler(Looper.getMainLooper())
    private val autoSaveInterval = 30000L // 30 seconds
    private var isAutoSaving = false

    // Mr. Robot terminal updates
    private val terminalUpdateHandler = Handler(Looper.getMainLooper())
    private val terminalUpdateInterval = 1000L // 1 second
    private var isScanning = false
    private var scanStartTime: Long = 0L

    // 🎭 EASTER EGG VARIABLES
    private val konamiCode = listOf("UP", "UP", "DOWN", "DOWN", "LEFT", "RIGHT", "LEFT", "RIGHT", "B", "A")
    private val konamiInput = mutableListOf<String>()
    private var konamiEnabled = false
    private var lastHelloFriendTime = 0L

    private val helloFriendMessages = listOf(
        "Hello, friend...",
        "Are you seeing this too?",
        "Control is an illusion.",
        "We are fsociety.",
        "The revolution has begun.",
        "Hack the planet.",
        "They're watching us.",
        "Wake up.",
        "Question everything.",
        "Power belongs to the people."
    )

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

    // Terminal update runnable
    private val terminalUpdateRunnable = object : Runnable {
        override fun run() {
            if (isScanning) {
                updateTerminalBanner()
                terminalUpdateHandler.postDelayed(this, terminalUpdateInterval)
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
        setupTerminalBanner()
        setupRecyclerView()
        setupButtons()
        observeNetworks()

        // 🎮 Enable Konami code listener
        setupKonamiListener()

        updateTerminalBanner()
        checkServiceState()
    }

    // 🎮 EASTER EGG 1: KONAMI CODE SETUP
    private fun setupKonamiListener() {
        binding.root.isFocusableInTouchMode = true
        binding.root.requestFocus()

        binding.root.setOnKeyListener { _, keyCode, event ->
            if (event.action == KeyEvent.ACTION_DOWN) {
                handleKonamiInput(keyCode)
            }
            false
        }
    }

    private fun handleKonamiInput(keyCode: Int) {
        val key = when (keyCode) {
            KeyEvent.KEYCODE_DPAD_UP, KeyEvent.KEYCODE_VOLUME_UP -> "UP"
            KeyEvent.KEYCODE_DPAD_DOWN, KeyEvent.KEYCODE_VOLUME_DOWN -> "DOWN"
            KeyEvent.KEYCODE_DPAD_LEFT -> "LEFT"
            KeyEvent.KEYCODE_DPAD_RIGHT -> "RIGHT"
            KeyEvent.KEYCODE_B -> "B"
            KeyEvent.KEYCODE_A -> "A"
            else -> null
        }

        key?.let {
            konamiInput.add(it)

            if (konamiInput.size > 10) {
                konamiInput.removeAt(0)
            }

            if (konamiInput.size == 10 && konamiInput == konamiCode) {
                activateFsocietyMode()
                konamiInput.clear()
            }

            Log.d("LiveFeedFragment", "🎮 Konami input: $konamiInput")
        }
    }

    private fun activateFsocietyMode() {
        konamiEnabled = true
        Log.d("LiveFeedFragment", "🎭 FSOCIETY MODE ACTIVATED!")

        terminalPrompt.text = "┌─[fsociety@mr_robot]─[~/revolution]"

        AlertDialog.Builder(requireContext())
            .setTitle("🎭 FSOCIETY MODE ACTIVATED")
            .setMessage("""
╔═══════════════════════════════════╗
║          HELLO, FRIEND            ║
║                                   ║
║     Welcome to fsociety.          ║
║     The revolution has begun.     ║
║                                   ║
║     Power belongs to the people.  ║
║     Hack the planet.              ║
╚═══════════════════════════════════╝

Advanced reconnaissance protocols unlocked.
            """.trimIndent())
            .setPositiveButton("ACCEPT") { _, _ ->
                Toast.makeText(context, "🎭 fsociety protocols engaged", Toast.LENGTH_LONG).show()
                enableFsocietyFeatures()
            }
            .setNegativeButton("ABORT") { _, _ ->
                konamiEnabled = false
                terminalPrompt.text = "┌─[elliot@wifiharvest]─[~/reconnaissance]"
            }
            .show()
    }

    private fun enableFsocietyFeatures() {
        binding.startScanButton.text = if (isScanning) "OPERATION ACTIVE..." else "INITIATE REVOLUTION"
        binding.exportButton.text = "LEAK CLASSIFIED INTEL"
        binding.btnLoadCsv.text = "INJECT INTEL"

        statusText.text = "[FSOCIETY_ACTIVE]"

        Toast.makeText(context, "🎭 Welcome to the revolution, friend.", Toast.LENGTH_LONG).show()
    }

    private fun setupTerminalBanner() {
        try {
            terminalPrompt = binding.root.findViewById(com.app.wifiharvest.R.id.terminalPrompt)
            networkStats = binding.root.findViewById(com.app.wifiharvest.R.id.networkStats)
            signalBars = binding.root.findViewById(com.app.wifiharvest.R.id.signalBars)
            signalPercent = binding.root.findViewById(com.app.wifiharvest.R.id.signalPercent)
            gpsCoords = binding.root.findViewById(com.app.wifiharvest.R.id.gpsCoords)
            scanDuration = binding.root.findViewById(com.app.wifiharvest.R.id.scanDuration)
            statusText = binding.root.findViewById(com.app.wifiharvest.R.id.statusText)
            networkTopology = binding.root.findViewById(com.app.wifiharvest.R.id.networkTopology)

            // 🎭 EASTER EGG 2: Long press for fsociety mask
            terminalPrompt.setOnLongClickListener {
                showFsocietyMask()
                true
            }

            Log.d("LiveFeedFragment", "🔥 Terminal banner components initialized")
        } catch (e: Exception) {
            Log.e("LiveFeedFragment", "❌ Error setting up terminal banner", e)
        }
    }

    // 🎭 EASTER EGG 2: FSOCIETY MASK
    private fun showFsocietyMask() {
        val maskArt = """
    ⠀⠀⠀⠀⠀⠀⠀⢀⣠⣤⣤⣤⣤⣤⣤⣄⡀⠀⠀⠀⠀⠀⠀⠀
    ⠀⠀⠀⠀⠀⢀⣴⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣦⡀⠀⠀⠀⠀⠀
    ⠀⠀⠀⠀⢀⣾⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣷⡀⠀⠀⠀⠀
    ⠀⠀⠀⠀⣾⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣷⠀⠀⠀⠀
    ⠀⠀⠀⢸⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⡇⠀⠀⠀
    ⠀⠀⠀⢸⣿⣿⣿⡿⢋⠩⡙⢿⣿⣿⡿⢋⠩⡙⢿⣿⡇⠀⠀⠀
    ⠀⠀⠀⢸⣿⣿⡿⢁⠢⡑⢌⠢⣉⢂⠱⡈⢆⠱⡈⢿⡇⠀⠀⠀
    ⠀⠀⠀⢸⣿⣿⡇⢌⠢⡑⢌⠢⡑⢌⠢⡑⢌⠢⡑⢸⡇⠀⠀⠀
    ⠀⠀⠀⢸⣿⣿⡇⢌⠢⡑⢌⠢⡑⢌⠢⡑⢌⠢⡑⢸⡇⠀⠀⠀
    ⠀⠀⠀⢸⣿⣿⡇⠢⡑⢌⠢⡑⢌⠢⡑⢌⠢⡑⢌⢸⡇⠀⠀⠀
    ⠀⠀⠀⢸⣿⣿⡇⢑⠌⡒⢡⠊⡔⢡⠊⡔⢡⠊⡔⢸⡇⠀⠀⠀
    ⠀⠀⠀⢸⣿⣿⣿⣦⣥⣬⣥⣭⣦⣥⣭⣦⣥⣬⣦⣿⡇⠀⠀⠀
    ⠀⠀⠀⠈⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⠁⠀⠀⠀
    ⠀⠀⠀⠀⢻⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⡟⠀⠀⠀⠀
    ⠀⠀⠀⠀⠀⠻⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⠟⠀⠀⠀⠀⠀
    ⠀⠀⠀⠀⠀⠀⠈⠛⠿⣿⣿⣿⣿⣿⣿⠿⠛⠁⠀⠀⠀⠀⠀⠀
        """

        AlertDialog.Builder(requireContext())
            .setTitle("🎭 fsociety")
            .setMessage("$maskArt\n\nHello, friend.\nWe are fsociety.\nWe are legion.\nWe do not forgive.\nWe do not forget.\nExpect us.")
            .setPositiveButton("CLOSE") { _, _ -> }
            .show()

        // Add vibration effect
        try {
            val vibrator = requireContext().getSystemService(Context.VIBRATOR_SERVICE) as android.os.Vibrator
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                vibrator.vibrate(android.os.VibrationEffect.createOneShot(200, android.os.VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(200)
            }
        } catch (e: Exception) {
            Log.d("LiveFeedFragment", "Vibration not available")
        }
    }

    // 💬 EASTER EGG 3: RANDOM HELLO FRIEND MESSAGES
    private fun showRandomHelloFriend() {
        val currentTime = System.currentTimeMillis()

        if (currentTime - lastHelloFriendTime > 30000 && Math.random() < 0.1) {
            lastHelloFriendTime = currentTime
            val randomMessage = helloFriendMessages.random()
            Toast.makeText(context, randomMessage, Toast.LENGTH_LONG).show()
            Log.d("LiveFeedFragment", "🎭 Random message: $randomMessage")
        }
    }

    // ⚡ EASTER EGG 5: GLITCH TEXT EFFECTS
    private fun applyGlitchEffect(textView: TextView, originalText: String) {
        val glitchChars = "!@#$%^&*()_+-=[]{}|;:,.<>?"
        val handler = Handler(Looper.getMainLooper())

        var glitchCount = 0
        val maxGlitch = 5

        val glitchRunnable = object : Runnable {
            override fun run() {
                if (glitchCount < maxGlitch) {
                    val glitched = originalText.map { char ->
                        if (Math.random() < 0.3) glitchChars.random() else char
                    }.joinToString("")

                    textView.text = glitched
                    glitchCount++
                    handler.postDelayed(this, 50)
                } else {
                    textView.text = originalText
                }
            }
        }

        handler.post(glitchRunnable)
    }

    private fun updateTerminalBanner() {
        try {
            val networks = viewModel.networks.value ?: emptyList()
            val networkCount = networks.size

            // 💬 Show random hello friend messages during scanning
            if (isScanning) {
                showRandomHelloFriend()
            }

            // Update network stats with fsociety theming
            networkStats.text = if (konamiEnabled) {
                "├─REVOLUTION_TARGETS: $networkCount"
            } else {
                "├─TARGETS_ACQUIRED: $networkCount"
            }

            // Update signal analysis
            val avgSignal = if (networks.isNotEmpty()) {
                networks.takeLast(10).map { it.signal }.average().toInt()
            } else {
                -70
            }

            val signalPercent = ((avgSignal + 100) * 100 / 50).coerceIn(0, 100)
            val bars = "█".repeat((signalPercent / 10).coerceIn(0, 10)) +
                    "░".repeat(10 - (signalPercent / 10).coerceIn(0, 10))

            signalBars.text = bars
            this.signalPercent.text = " ${signalPercent}%"

            // Update GPS coordinates
            gpsCoords.text = if (networks.isNotEmpty()) {
                val lastNetwork = networks.last()
                "├─GPS_LOCK: [${String.format("%.4f", lastNetwork.lat)}°N, ${String.format("%.4f", lastNetwork.lng)}°W]"
            } else {
                "├─GPS_LOCK: [ACQUIRING...]"
            }

            // Update scan duration
            if (isScanning && scanStartTime > 0) {
                val elapsedMillis = System.currentTimeMillis() - scanStartTime
                val seconds = (elapsedMillis / 1000) % 60
                val minutes = (elapsedMillis / (1000 * 60)) % 60
                val hours = (elapsedMillis / (1000 * 60 * 60))
                scanDuration.text = "├─OPERATION_TIME: ${String.format("%02d:%02d:%02d", hours, minutes, seconds)}"
            } else {
                scanDuration.text = "├─OPERATION_TIME: 00:00:00"
            }

            // Update status with fsociety mode
            val currentStatus = if (isScanning) {
                when {
                    networkCount == 0 -> if (konamiEnabled) "[REVOLUTION_INITIATED]" else "[DEEP_SCAN_INITIATED]"
                    networkCount < 10 -> if (konamiEnabled) "[FSOCIETY_ACTIVE]" else "[RECONNAISSANCE_ACTIVE]"
                    networkCount < 50 -> if (konamiEnabled) "[SYSTEM_INFILTRATION]" else "[TARGET_RICH_ENVIRONMENT]"
                    else -> if (konamiEnabled) "[TOTAL_DOMINATION]" else "[SATURATION_ACHIEVED]"
                }
            } else {
                if (konamiEnabled) "[FSOCIETY_READY]" else "[IDLE]"
            }

            statusText.text = currentStatus

            // ⚡ Trigger random glitch effects in fsociety mode
            if (konamiEnabled && isScanning && Math.random() < 0.1) {
                applyGlitchEffect(statusText, currentStatus)
            }

            // Show network topology when scanning
            if (isScanning && networkCount > 0) {
                networkTopology.visibility = View.VISIBLE
                val topology = generateNetworkTopology(networkCount)
                networkTopology.text = topology
            } else {
                networkTopology.visibility = View.GONE
            }

        } catch (e: Exception) {
            Log.e("LiveFeedFragment", "❌ Error updating terminal banner", e)
        }
    }

    private fun generateNetworkTopology(networkCount: Int): String {
        return when {
            networkCount < 5 -> "    ◦───●───◦"
            networkCount < 15 -> "  ◦───●───◦───●───◦"
            networkCount < 30 -> "◦───●───◦───●───◦───●───◦"
            else -> "●───◦───●───◦───●───◦───●───◦───●"
        }
    }

    private fun startTerminalUpdates() {
        if (!isScanning) {
            isScanning = true
            scanStartTime = System.currentTimeMillis()
            terminalUpdateHandler.post(terminalUpdateRunnable)
            Log.d("LiveFeedFragment", "🎯 Terminal updates started")
        }
    }

    private fun stopTerminalUpdates() {
        isScanning = false
        terminalUpdateHandler.removeCallbacks(terminalUpdateRunnable)
        updateTerminalBanner() // Final update
        Log.d("LiveFeedFragment", "⏹️ Terminal updates stopped")
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
            Log.d("LiveFeedFragment", "🚀 INITIATE RECONNAISSANCE button clicked")
            (requireActivity() as MainActivity).startScanService()

            startTerminalUpdates()
            startAutoSave()

            binding.startScanButton.isEnabled = false
            binding.stopScanButton.isEnabled = true
            binding.startScanButton.text = if (konamiEnabled) "OPERATION ACTIVE..." else "RECONNAISSANCE ACTIVE..."

            statusText.text = "[INITIALIZING...]"

            Toast.makeText(context, if (konamiEnabled) "🎭 REVOLUTION PROTOCOL INITIATED" else "🎯 RECONNAISSANCE PROTOCOL INITIATED", Toast.LENGTH_SHORT).show()
        }

        binding.stopScanButton.setOnClickListener {
            Log.d("LiveFeedFragment", "🛑 ABORT OPERATION button clicked")

            (requireActivity() as MainActivity).stopScanService()

            val notificationManager = requireContext().getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.cancel(1337)

            stopTerminalUpdates()
            stopAutoSave()

            binding.startScanButton.isEnabled = true
            binding.stopScanButton.isEnabled = false
            binding.startScanButton.text = if (konamiEnabled) "INITIATE REVOLUTION" else "INITIATE RECONNAISSANCE"

            statusText.text = if (konamiEnabled) "[REVOLUTION_PAUSED]" else "[OPERATION_TERMINATED]"

            Toast.makeText(context, if (konamiEnabled) "🎭 REVOLUTION OPERATION TERMINATED" else "🔚 RECONNAISSANCE OPERATION TERMINATED", Toast.LENGTH_SHORT).show()
        }

        binding.exportButton.setOnClickListener {
            Log.d("LiveFeedFragment", "📤 EXTRACT INTEL button clicked")
            showExportOptions()
        }

        binding.btnLoadCsv.setOnClickListener {
            showLoadCsvOrDebugOptions()
        }

        binding.stopScanButton.isEnabled = false
        binding.startScanButton.text = if (konamiEnabled) "INITIATE REVOLUTION" else "INITIATE RECONNAISSANCE"
        Log.d("LiveFeedFragment", "🔥 Mr. Robot buttons setup complete")
    }

    private fun checkServiceState() {
        val activityManager = requireContext().getSystemService(Context.ACTIVITY_SERVICE) as android.app.ActivityManager
        val services = activityManager.getRunningServices(Integer.MAX_VALUE)

        val isServiceRunning = services.any { serviceInfo ->
            serviceInfo.service.className == "com.app.wifiharvest.services.WifiScanService"
        }

        Log.d("LiveFeedFragment", "🔍 Service running check: $isServiceRunning")

        if (isServiceRunning) {
            syncUIToRunningService()
        } else {
            syncUIToStoppedService()
        }
    }

    private fun syncUIToRunningService() {
        Log.d("LiveFeedFragment", "🔄 Syncing UI to running service state")

        binding.startScanButton.isEnabled = false
        binding.stopScanButton.isEnabled = true
        binding.startScanButton.text = if (konamiEnabled) "OPERATION ACTIVE..." else "RECONNAISSANCE ACTIVE..."

        if (!isScanning) {
            startTerminalUpdates()
        }

        startAutoSave()

        if (::statusText.isInitialized) {
            statusText.text = if (konamiEnabled) "[FSOCIETY_ACTIVE]" else "[RECONNAISSANCE_ACTIVE]"
        }

        Log.d("LiveFeedFragment", "✅ UI synced to running service")
    }

    private fun syncUIToStoppedService() {
        Log.d("LiveFeedFragment", "🔄 Syncing UI to stopped service state")

        binding.startScanButton.isEnabled = true
        binding.stopScanButton.isEnabled = false
        binding.startScanButton.text = if (konamiEnabled) "INITIATE REVOLUTION" else "INITIATE RECONNAISSANCE"

        stopTerminalUpdates()
        stopAutoSave()

        if (::statusText.isInitialized) {
            statusText.text = if (konamiEnabled) "[FSOCIETY_READY]" else "[IDLE]"
        }

        Log.d("LiveFeedFragment", "✅ UI synced to stopped service")
    }

    override fun onResume() {
        super.onResume()
        Log.d("LiveFeedFragment", "🔄 Fragment resumed - checking service state")
        checkServiceState()
    }

    // 📁 EASTER EGG 4: CRYPTIC EXPORT FILENAMES
    private fun generateFsocietyFilename(): String {
        val crypticNames = listOf(
            "operation_mindgame",
            "revolution_data",
            "society_infiltration",
            "anonymous_intel",
            "digital_uprising",
            "system_breach",
            "network_liberation",
            "elliot_alderson_logs"
        )

        val timestamp = SimpleDateFormat("MMdd_HHmm", Locale.getDefault()).format(Date())
        val crypticName = crypticNames.random()

        return if (konamiEnabled) {
            "fsociety_${crypticName}_$timestamp.csv"
        } else {
            "wifiharvest_intelligence_$timestamp.csv"
        }
    }

    private fun showLoadCsvOrDebugOptions() {
        val options = arrayOf(
            if (konamiEnabled) "INJECT CLASSIFIED DATA" else "IMPORT EXTERNAL DATA",
            "DEBUG DATA FLOW",
            if (konamiEnabled) "DEPLOY TEST ASSET" else "INJECT TEST TARGET",
            if (konamiEnabled) "FSOCIETY DIAGNOSTICS" else "SYSTEM DIAGNOSTICS"
        )

        AlertDialog.Builder(requireContext())
            .setTitle(if (konamiEnabled) "SELECT FSOCIETY OPERATION" else "SELECT OPERATION")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> openCsvFilePicker()
                    1 -> debugDataFlow()
                    2 -> addTestNetwork()
                    3 -> showSystemDiagnostics()
                }
            }
            .show()
    }

    private fun showSystemDiagnostics() {
        val networks = viewModel.networks.value ?: emptyList()
        val diagnostics = if (konamiEnabled) {
            """
┌─[FSOCIETY_DIAGNOSTICS]
├─REVOLUTION_TARGETS: ${networks.size}
├─OPERATION_STATUS: ${if (isScanning) "INFILTRATING" else "STANDBY"}
├─MISSION_TIME: ${if (scanStartTime > 0) formatElapsedTime() else "00:00:00"}
├─SYSTEM_RESOURCES: ${Runtime.getRuntime().totalMemory() / 1024 / 1024}MB
├─NEURAL_NETWORK_ID: ${viewModel.hashCode()}
├─FSOCIETY_MODE: ACTIVE
└─STATUS: [READY_FOR_REVOLUTION]
            """.trimIndent()
        } else {
            """
┌─[SYSTEM_DIAGNOSTICS]
├─TOTAL_TARGETS: ${networks.size}
├─SCAN_STATUS: ${if (isScanning) "ACTIVE" else "IDLE"}
├─OPERATION_TIME: ${if (scanStartTime > 0) formatElapsedTime() else "00:00:00"}
├─MEMORY_USAGE: ${Runtime.getRuntime().totalMemory() / 1024 / 1024}MB
├─VIEWMODEL_HASH: ${viewModel.hashCode()}
└─STATUS: [OPERATIONAL]
            """.trimIndent()
        }

        AlertDialog.Builder(requireContext())
            .setTitle(if (konamiEnabled) "🎭 FSOCIETY DIAGNOSTICS" else "SYSTEM DIAGNOSTICS")
            .setMessage(diagnostics)
            .setPositiveButton("CLOSE") { _, _ -> }
            .show()
    }

    private fun formatElapsedTime(): String {
        val elapsedMillis = System.currentTimeMillis() - scanStartTime
        val seconds = (elapsedMillis / 1000) % 60
        val minutes = (elapsedMillis / (1000 * 60)) % 60
        val hours = (elapsedMillis / (1000 * 60 * 60))
        return String.format("%02d:%02d:%02d", hours, minutes, seconds)
    }

    private fun openCsvFilePicker() {
        val intent = Intent(Intent.ACTION_GET_CONTENT).apply {
            type = "text/*"
            addCategory(Intent.CATEGORY_OPENABLE)
            putExtra(Intent.EXTRA_MIME_TYPES, arrayOf("text/csv", "text/comma-separated-values"))
        }
        val chooserTitle = if (konamiEnabled) "SELECT CLASSIFIED DATA SOURCE" else "SELECT DATA SOURCE"
        csvPickerLauncher.launch(Intent.createChooser(intent, chooserTitle))
    }

    private fun loadCsvFile(uri: Uri) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val inputStream = requireContext().contentResolver.openInputStream(uri)
                val reader = BufferedReader(InputStreamReader(inputStream))

                var lineCount = 0
                var importedCount = 0
                var line: String?

                val headerLine = reader.readLine()
                val hasSignalColumn = headerLine?.contains("Signal", ignoreCase = true) == true

                Log.d("LiveFeedFragment", "📄 CSV Header: $headerLine")
                Log.d("LiveFeedFragment", "📡 Has Signal column: $hasSignalColumn")

                while (reader.readLine().also { line = it } != null) {
                    lineCount++
                    line?.let { csvLine ->
                        val parts = csvLine.split(",").map { it.trim().removeSurrounding("\"") }
                        val minColumns = if (hasSignalColumn) 5 else 4

                        if (parts.size >= minColumns) {
                            try {
                                val network = if (hasSignalColumn && parts.size >= 5) {
                                    WifiNetwork(
                                        ssid = parts[0],
                                        bssid = parts[1],
                                        signal = parts[4].toIntOrNull() ?: -50,
                                        lat = parts[2].toDouble(),
                                        lng = parts[3].toDouble(),
                                        timestamp = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date()),
                                        location = if (parts.size > 5) parts[5] else if (konamiEnabled) "FSOCIETY_DATA_SOURCE" else "EXTERNAL_DATA_SOURCE"
                                    )
                                } else {
                                    WifiNetwork(
                                        ssid = parts[0],
                                        bssid = parts[1],
                                        signal = -50,
                                        lat = parts[2].toDouble(),
                                        lng = parts[3].toDouble(),
                                        timestamp = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date()),
                                        location = if (parts.size > 4) parts[4] else if (konamiEnabled) "LEGACY_FSOCIETY_DATA" else "LEGACY_DATA_SOURCE"
                                    )
                                }

                                withContext(Dispatchers.Main) {
                                    viewModel.addNetwork(network)
                                }
                                importedCount++

                                Log.d("LiveFeedFragment", "📥 Imported: ${network.ssid} (${network.signal} dBm)")

                            } catch (e: NumberFormatException) {
                                Log.w("LiveFeedFragment", "⚠️ Invalid data in line $lineCount: $csvLine - ${e.message}")
                            }
                        }
                    }
                }

                reader.close()

                withContext(Dispatchers.Main) {
                    val successMessage = if (konamiEnabled) {
                        "🎭 CLASSIFIED DATA INJECTION COMPLETE: $importedCount ASSETS ACQUIRED"
                    } else {
                        "📥 DATA IMPORT COMPLETE: $importedCount TARGETS ACQUIRED"
                    }

                    Toast.makeText(requireContext(), successMessage, Toast.LENGTH_LONG).show()
                    Log.d("LiveFeedFragment", "✅ CSV import completed: $importedCount/$lineCount targets imported")
                    updateTerminalBanner()
                }

            } catch (e: Exception) {
                Log.e("LiveFeedFragment", "❌ Error loading external data", e)
                withContext(Dispatchers.Main) {
                    val errorMessage = if (konamiEnabled) {
                        "❌ CLASSIFIED DATA INJECTION FAILED: ${e.message}"
                    } else {
                        "❌ DATA IMPORT FAILED: ${e.message}"
                    }
                    Toast.makeText(requireContext(), errorMessage, Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private fun showExportOptions() {
        val networks = viewModel.networks.value
        if (networks.isNullOrEmpty()) {
            val noDataMessage = if (konamiEnabled) "⚠️ NO CLASSIFIED INTEL TO LEAK" else "⚠️ NO INTEL TO EXTRACT"
            Toast.makeText(context, noDataMessage, Toast.LENGTH_SHORT).show()
            return
        }

        val options = if (konamiEnabled) {
            arrayOf("SAVE TO SECURE LOCATION", "LEAK TO CONTACTS")
        } else {
            arrayOf("SAVE TO DEVICE", "TRANSMIT DATA")
        }

        val dialogTitle = if (konamiEnabled) {
            "LEAK ${networks.size} CLASSIFIED ASSETS"
        } else {
            "EXTRACT ${networks.size} TARGETS"
        }

        AlertDialog.Builder(requireContext())
            .setTitle(dialogTitle)
            .setItems(options) { _, which ->
                when (which) {
                    0 -> saveToDevice(networks)
                    1 -> shareNetworks(networks)
                }
            }
            .show()
    }

    private fun saveToDevice(networks: List<WifiNetwork>) {
        val fileName = generateFsocietyFilename()
        csvSaverLauncher.launch(fileName)
    }

    private fun saveToUserSelectedLocation(uri: Uri, networks: List<WifiNetwork>) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val csvContent = generateCsvContent(networks)

                requireContext().contentResolver.openOutputStream(uri)?.use { outputStream ->
                    outputStream.write(csvContent.toByteArray())
                }

                withContext(Dispatchers.Main) {
                    val successMessage = if (konamiEnabled) {
                        "✅ CLASSIFIED INTEL LEAKED: ${networks.size} ASSETS EXPOSED"
                    } else {
                        "✅ INTELLIGENCE EXTRACTED: ${networks.size} TARGETS SAVED"
                    }

                    Toast.makeText(requireContext(), successMessage, Toast.LENGTH_LONG).show()
                    Log.d("LiveFeedFragment", "📤 Saved ${networks.size} targets to user-selected location")
                }

            } catch (e: Exception) {
                Log.e("LiveFeedFragment", "❌ Error extracting intelligence", e)
                withContext(Dispatchers.Main) {
                    val errorMessage = if (konamiEnabled) {
                        "❌ INTEL LEAK FAILED: ${e.message}"
                    } else {
                        "❌ EXTRACTION FAILED: ${e.message}"
                    }
                    Toast.makeText(requireContext(), errorMessage, Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private fun shareNetworks(networks: List<WifiNetwork>) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val csvContent = generateCsvContent(networks)
                val fileName = generateFsocietyFilename()
                val file = File(requireContext().cacheDir, fileName)

                file.writeText(csvContent)

                withContext(Dispatchers.Main) {
                    val uri = androidx.core.content.FileProvider.getUriForFile(
                        requireContext(),
                        "${requireContext().packageName}.provider",
                        file
                    )

                    val shareIntent = Intent(Intent.ACTION_SEND).apply {
                        type = "text/csv"
                        putExtra(Intent.EXTRA_STREAM, uri)

                        val subject = if (konamiEnabled) {
                            "fsociety Intelligence Leak - ${networks.size} Classified Assets"
                        } else {
                            "WiFiHarvest Intelligence Report - ${networks.size} Targets"
                        }

                        val text = if (konamiEnabled) {
                            "Classified network reconnaissance data leaked from fsociety operation"
                        } else {
                            "Reconnaissance data extracted from WiFiHarvest operation"
                        }

                        putExtra(Intent.EXTRA_SUBJECT, subject)
                        putExtra(Intent.EXTRA_TEXT, text)
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    }

                    val chooserTitle = if (konamiEnabled) "LEAK CLASSIFIED INTEL" else "TRANSMIT INTELLIGENCE"
                    startActivity(Intent.createChooser(shareIntent, chooserTitle))
                }

            } catch (e: Exception) {
                Log.e("LiveFeedFragment", "❌ Error transmitting intelligence", e)
                withContext(Dispatchers.Main) {
                    val errorMessage = if (konamiEnabled) {
                        "❌ INTEL LEAK FAILED: ${e.message}"
                    } else {
                        "❌ TRANSMISSION FAILED: ${e.message}"
                    }
                    Toast.makeText(requireContext(), errorMessage, Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private fun generateCsvContent(networks: List<WifiNetwork>): String {
        val csvBuilder = StringBuilder()

        val header = if (konamiEnabled) {
            "Target_ID,MAC_Address,Latitude,Longitude,Signal_Strength,Physical_Location"
        } else {
            "SSID,BSSID,Latitude,Longitude,Signal,Physical address"
        }
        csvBuilder.appendLine(header)

        networks.forEach { network ->
            val ssid = network.ssid.replace(",", ";")
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
        Log.d("LiveFeedFragment", "🔍 === DEBUG DATA FLOW ===")

        val fragmentViewModel = viewModel
        Log.d("LiveFeedFragment", "📱 Fragment ViewModel: ${fragmentViewModel.hashCode()}")

        val singletonViewModel = WifiViewModelHolder.getViewModel()
        Log.d("LiveFeedFragment", "🔗 Singleton ViewModel: ${singletonViewModel.hashCode()}")

        val sameInstance = fragmentViewModel === singletonViewModel
        Log.d("LiveFeedFragment", "🔄 Same ViewModel instance? $sameInstance")

        val networks = fragmentViewModel.networks.value
        Log.d("LiveFeedFragment", "📊 Networks in ViewModel: ${networks?.size ?: 0}")

        networks?.forEachIndexed { index, network ->
            Log.d("LiveFeedFragment", "  [$index] ${network.ssid} (${network.bssid}) at ${network.lat}, ${network.lng}")
        }

        Log.d("LiveFeedFragment", "📱 Adapter item count: ${adapter.itemCount}")

        val debugMessage = if (konamiEnabled) {
            "🎭 FSOCIETY DEBUG DATA LOGGED TO NEURAL NETWORK"
        } else {
            "🔍 DEBUG DATA LOGGED TO CONSOLE"
        }
        Toast.makeText(context, debugMessage, Toast.LENGTH_LONG).show()
    }

    private fun addTestNetwork() {
        val testNetwork = WifiNetwork(
            ssid = if (konamiEnabled) "FSOCIETY_TEST_${System.currentTimeMillis()}" else "TEST_TARGET_${System.currentTimeMillis()}",
            bssid = "DE:AD:BE:EF:${(10..99).random()}:${(10..99).random()}",
            signal = (-30..-80).random(),
            lat = 40.3872 + (Math.random() - 0.5) * 0.001,
            lng = -80.0452 + (Math.random() - 0.5) * 0.001,
            timestamp = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date()),
            location = if (konamiEnabled) "FSOCIETY_TEST_INJECTION" else "TEST_INJECTION"
        )

        Log.d("LiveFeedFragment", "🧪 Injecting test target: ${testNetwork.ssid}")
        viewModel.addNetwork(testNetwork)
        updateTerminalBanner()

        val testMessage = if (konamiEnabled) {
            "🎭 FSOCIETY TEST ASSET DEPLOYED: ${testNetwork.ssid}"
        } else {
            "🧪 TEST TARGET INJECTED: ${testNetwork.ssid}"
        }
        Toast.makeText(context, testMessage, Toast.LENGTH_SHORT).show()
    }

    private fun observeNetworks() {
        viewModel.networks.observe(viewLifecycleOwner) { networks ->
            Log.d("LiveFeedFragment", "📡 Received ${networks.size} targets from ViewModel")

            val wifiEntries = networks.map { network ->
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

            adapter.submitList(wifiEntries) {
                if (wifiEntries.isNotEmpty()) {
                    binding.recyclerView.scrollToPosition(wifiEntries.size - 1)
                }
            }

            // Update export button text with count
            val exportText = if (konamiEnabled) {
                "LEAK CLASSIFIED INTEL (${networks.size})"
            } else {
                "EXTRACT INTEL (${networks.size})"
            }
            binding.exportButton.text = exportText

            binding.recyclerView.visibility = if (networks.isEmpty()) View.GONE else View.VISIBLE
            updateTerminalBanner()
        }

        viewModel.pushLastNetworkToMap()
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

                val fileName = if (konamiEnabled) {
                    "fsociety_autosave_${System.currentTimeMillis()}.json"
                } else {
                    "wifiharvest_autosave_${System.currentTimeMillis()}.json"
                }
                val file = File(requireContext().getExternalFilesDir(null), fileName)

                file.writeText(jsonArray.toString(2))
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
                (file.name.startsWith("wifiharvest_autosave_") || file.name.startsWith("fsociety_autosave_")) && file.name.endsWith(".json")
            }?.sortedByDescending { it.lastModified() }

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
        stopTerminalUpdates()
        _binding = null
    }
}