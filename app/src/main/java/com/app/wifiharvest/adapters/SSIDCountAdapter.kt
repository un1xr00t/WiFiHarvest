package com.app.wifiharvest.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.app.wifiharvest.databinding.ItemSsidCountBinding

class SSIDCountAdapter : RecyclerView.Adapter<SSIDCountAdapter.ViewHolder>() {

    private val items = mutableListOf<Pair<String, Int>>()

    fun submitList(data: List<Pair<String, Int>>) {
        items.clear()
        items.addAll(data)
        notifyDataSetChanged()
    }

    inner class ViewHolder(val binding: ItemSsidCountBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemSsidCountBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun getItemCount(): Int = items.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val (ssid, count) = items[position]
        holder.binding.ssidName.text = ssid
        holder.binding.ssidCount.text = count.toString()
    }
}
