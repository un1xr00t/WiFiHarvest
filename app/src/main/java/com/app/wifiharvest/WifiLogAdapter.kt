package com.app.wifiharvest

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.app.wifiharvest.models.WifiEntry

class WifiLogAdapter :
    ListAdapter<WifiEntry, WifiLogAdapter.ViewHolder>(DiffCallback) {

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val ssidText: TextView = itemView.findViewById(R.id.ssidText)
        val bssidText: TextView = itemView.findViewById(R.id.bssidText)
        val coordsText: TextView = itemView.findViewById(R.id.coordsText)
        val signalText: TextView = itemView.findViewById(R.id.signalText)
        val timestampText: TextView = itemView.findViewById(R.id.timestampText)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_wifi_log, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val entry = getItem(position)

        // Basic info
        holder.ssidText.text = if (entry.SSID.isBlank()) "(Hidden Network)" else entry.SSID
        holder.bssidText.text = entry.BSSID
        holder.coordsText.text = "%.6f, %.6f".format(entry.lat, entry.lng)
        holder.timestampText.text = entry.timestamp ?: "No timestamp"

        // Signal with color coding
        val signalColor = when {
            entry.signal >= -50 -> Color.parseColor("#00FF00") // Green - Excellent
            entry.signal >= -65 -> Color.parseColor("#7FFF00") // Light Green - Good
            entry.signal >= -75 -> Color.parseColor("#FFFF00") // Yellow - Fair
            entry.signal >= -85 -> Color.parseColor("#FF7F00") // Orange - Poor
            else -> Color.parseColor("#FF0000") // Red - Very Poor
        }

        holder.signalText.text = "${entry.signal} dBm"
        holder.signalText.setTextColor(signalColor)

        android.util.Log.d("WifiLogAdapter", "Bound entry: ${entry.SSID} (${entry.BSSID})")
    }
}

// DiffUtil callback for efficient updates
private val DiffCallback = object : DiffUtil.ItemCallback<WifiEntry>() {
    override fun areItemsTheSame(oldItem: WifiEntry, newItem: WifiEntry): Boolean {
        return oldItem.BSSID == newItem.BSSID
    }

    override fun areContentsTheSame(oldItem: WifiEntry, newItem: WifiEntry): Boolean {
        return oldItem == newItem
    }
}