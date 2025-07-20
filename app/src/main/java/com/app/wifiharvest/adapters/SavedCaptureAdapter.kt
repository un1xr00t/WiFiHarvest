package com.app.wifiharvest.adapters

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.app.wifiharvest.databinding.ItemCaptureFileBinding
import java.io.File

class SavedCaptureAdapter(
    private val context: Context,
    private val files: List<File>,
    private val onClick: (File) -> Unit
) : RecyclerView.Adapter<SavedCaptureAdapter.ViewHolder>() {

    inner class ViewHolder(val binding: ItemCaptureFileBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemCaptureFileBinding.inflate(LayoutInflater.from(context), parent, false)
        return ViewHolder(binding)
    }

    override fun getItemCount() = files.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val file = files[position]
        holder.binding.fileName.text = file.name
        holder.binding.root.setOnClickListener { onClick(file) }
    }
}
