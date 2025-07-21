package com.app.wifiharvest.fragments

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.app.wifiharvest.R
import com.app.wifiharvest.WifiNetwork

class TopNetworksAdapter : ListAdapter<WifiNetwork, TopNetworksAdapter.ViewHolder>(DiffCallback) {

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val rankText: TextView = itemView.findViewById(R.id.rankText)
        val ssidText: TextView = itemView.findViewById(R.id.topNetworkSSID)
        val signalText: TextView = itemView.findViewById(R.id.topNetworkSignal)
        val bssidText: TextView = itemView.findViewById(R.id.topNetworkBSSID)
        val signalBar: View = itemView.findViewById(R.id.signalBar)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_top_network, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val network = getItem(position)

        // Rank
        holder.rankText.text = "#${position + 1}"

        // SSID
        holder.ssidText.text = network.ssid

        // Signal with color coding
        val signalColor = when {
            network.signal >= -50 -> Color.parseColor("#00FF00") // Green
            network.signal >= -65 -> Color.parseColor("#7FFF00") // Light Green
            network.signal >= -75 -> Color.parseColor("#FFFF00") // Yellow
            network.signal >= -85 -> Color.parseColor("#FF7F00") // Orange
            else -> Color.parseColor("#FF0000") // Red
        }

        holder.signalText.text = "${network.signal} dBm"
        holder.signalText.setTextColor(signalColor)

        // BSSID
        holder.bssidText.text = network.bssid

        // Signal strength bar
        val signalPercentage = ((network.signal + 100) / 50.0).coerceIn(0.0, 1.0)
        val layoutParams = holder.signalBar.layoutParams
        layoutParams.width = (200 * signalPercentage).toInt() // Max width 200dp
        holder.signalBar.layoutParams = layoutParams
        holder.signalBar.setBackgroundColor(signalColor)

        // Trophy icon for top 3
        holder.rankText.setTextColor(when(position) {
            0 -> Color.parseColor("#FFD700") // Gold
            1 -> Color.parseColor("#C0C0C0") // Silver
            2 -> Color.parseColor("#CD7F32") // Bronze
            else -> Color.WHITE
        })
    }

    companion object {
        private val DiffCallback = object : DiffUtil.ItemCallback<WifiNetwork>() {
            override fun areItemsTheSame(oldItem: WifiNetwork, newItem: WifiNetwork): Boolean {
                return oldItem.bssid == newItem.bssid
            }

            override fun areContentsTheSame(oldItem: WifiNetwork, newItem: WifiNetwork): Boolean {
                return oldItem == newItem
            }
        }
    }
}