package com.app.wifiharvest

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

data class WifiLogEntry(
    val ssid: String,
    val bssid: String,
    val address: String? = null
)


class WifiLogAdapter : RecyclerView.Adapter<WifiLogAdapter.ViewHolder>() {
    private val data = mutableListOf<WifiLogEntry>()

    fun addEntry(entry: WifiLogEntry) {
        data.add(0, entry)
        notifyItemInserted(0)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view =
            LayoutInflater.from(parent.context).inflate(R.layout.item_wifi_log, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val entry = data[position]
        holder.ssidBssid.text = "${entry.ssid} (${entry.bssid})"
        holder.address.text = entry.address ?: "Locating..."
    }

    override fun getItemCount(): Int = data.size


    class ViewHolder(v: View) : RecyclerView.ViewHolder(v) {
        val ssidBssid: TextView = v.findViewById(R.id.ssid_bssid)
        val address: TextView = v.findViewById(R.id.address)
    }
}