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
        val locationText: TextView = itemView.findViewById(R.id.locationText)
        val coordsText: TextView = itemView.findViewById(R.id.coordsText)
        val timestampText: TextView = itemView.findViewById(R.id.timestampText)
        val signalText: TextView = itemView.findViewById(R.id.signalText)

        // Signal strength bars
        val bar1: View = itemView.findViewById(R.id.bar1)
        val bar2: View = itemView.findViewById(R.id.bar2)
        val bar3: View = itemView.findViewById(R.id.bar3)
        val bar4: View = itemView.findViewById(R.id.bar4)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_wifi_log, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val entry = getItem(position)

        // Network Name (SSID) - Green
        holder.ssidText.text = if (entry.SSID.isBlank()) "(Hidden Network)" else entry.SSID
        holder.ssidText.setTextColor(Color.parseColor("#00FF00"))

        // MAC Address (BSSID) - Green
        holder.bssidText.text = entry.BSSID
        holder.bssidText.setTextColor(Color.parseColor("#00FF00"))

        // Physical Address - Green
        val physicalAddress = entry.location ?: "Resolving address..."
        holder.locationText.text = physicalAddress
        holder.locationText.setTextColor(Color.parseColor("#00FF00"))

        // Coordinates - Green
        holder.coordsText.text = "%.6f, %.6f".format(entry.lat, entry.lng)
        holder.coordsText.setTextColor(Color.parseColor("#00FF00"))

        // Date/Timestamp - Green
        holder.timestampText.text = entry.timestamp ?: "No timestamp"
        holder.timestampText.setTextColor(Color.parseColor("#00FF00"))

        // Signal strength with color coding and bars
        val signalLevel = when {
            entry.signal >= -50 -> 4 // Excellent
            entry.signal >= -65 -> 3 // Good
            entry.signal >= -75 -> 2 // Fair
            entry.signal >= -85 -> 1 // Poor
            else -> 0 // Very Poor
        }

        val signalColor = when (signalLevel) {
            4 -> Color.parseColor("#00FF00") // Green - Excellent
            3 -> Color.parseColor("#7FFF00") // Light Green - Good
            2 -> Color.parseColor("#FFFF00") // Yellow - Fair
            1 -> Color.parseColor("#FF7F00") // Orange - Poor
            else -> Color.parseColor("#FF0000") // Red - Very Poor
        }

        holder.signalText.text = "${entry.signal} dBm"
        holder.signalText.setTextColor(signalColor)

        // Update signal bars
        updateSignalBars(holder, signalLevel, signalColor)

        android.util.Log.d("WifiLogAdapter", "Bound entry: ${entry.SSID} (${entry.BSSID}) at ${physicalAddress}")
    }

    private fun updateSignalBars(holder: ViewHolder, signalLevel: Int, signalColor: Int) {
        val inactiveColor = Color.parseColor("#666666")

        // Reset all bars to inactive
        holder.bar1.setBackgroundColor(inactiveColor)
        holder.bar2.setBackgroundColor(inactiveColor)
        holder.bar3.setBackgroundColor(inactiveColor)
        holder.bar4.setBackgroundColor(inactiveColor)

        // Light up bars based on signal level
        when (signalLevel) {
            4 -> {
                holder.bar1.setBackgroundColor(signalColor)
                holder.bar2.setBackgroundColor(signalColor)
                holder.bar3.setBackgroundColor(signalColor)
                holder.bar4.setBackgroundColor(signalColor)
            }
            3 -> {
                holder.bar1.setBackgroundColor(signalColor)
                holder.bar2.setBackgroundColor(signalColor)
                holder.bar3.setBackgroundColor(signalColor)
            }
            2 -> {
                holder.bar1.setBackgroundColor(signalColor)
                holder.bar2.setBackgroundColor(signalColor)
            }
            1 -> {
                holder.bar1.setBackgroundColor(signalColor)
            }
            // 0 - all bars remain inactive (gray)
        }
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