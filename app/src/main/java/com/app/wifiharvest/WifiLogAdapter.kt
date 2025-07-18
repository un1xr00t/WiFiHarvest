package com.app.wifiharvest

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

data class WifiLogEntry(val ssid: String, val bssid: String, val latitude: Double?, val longitude: Double?)

class WifiLogAdapter : RecyclerView.Adapter<WifiLogAdapter.ViewHolder>() {
    private val data = mutableListOf<WifiLogEntry>()

    fun addEntry(entry: WifiLogEntry) {
        data.add(0, entry)
        notifyItemInserted(0)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val v = LayoutInflater.from(parent.context).inflate(android.R.layout.simple_list_item_2, parent, false)
        return ViewHolder(v)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val entry = data[position]
        holder.line1.text = "${entry.ssid} (${entry.bssid})"
        holder.line2.text = "Lat: ${entry.latitude}, Lng: ${entry.longitude}"
    }

    override fun getItemCount(): Int = data.size

    class ViewHolder(v: View) : RecyclerView.ViewHolder(v) {
        val line1: TextView = v.findViewById(android.R.id.text1)
        val line2: TextView = v.findViewById(android.R.id.text2)
    }
}