package com.app.wifiharvest

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

data class WifiLogEntry(
    val ssid: String,
    val bssid: String,
    val timestamp: String,
    val location: String? = null  // Optional location
)

class WifiLogAdapter : RecyclerView.Adapter<WifiLogAdapter.LogViewHolder>() {

    private val wifiLogs = mutableListOf<WifiLogEntry>()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LogViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_wifi_log, parent, false)
        return LogViewHolder(view)
    }

    override fun onBindViewHolder(holder: LogViewHolder, position: Int) {
        holder.bind(wifiLogs[position])
    }

    override fun getItemCount(): Int = wifiLogs.size

    fun addLog(entry: WifiLogEntry) {
        wifiLogs.add(entry)
        notifyItemInserted(wifiLogs.size - 1)
    }

    inner class LogViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val ssidBssidView: TextView = view.findViewById(R.id.ssid_bssid)
        private val addressView: TextView = view.findViewById(R.id.address)

        fun bind(entry: WifiLogEntry) {
            ssidBssidView.text = "[${entry.timestamp}] ${entry.ssid} (${entry.bssid})"
            addressView.text = "↳ ${entry.location ?: "Unknown Location"}"
        }
    }
}
