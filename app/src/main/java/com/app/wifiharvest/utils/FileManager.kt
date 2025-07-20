package com.app.wifiharvest.utils

import android.content.Context
import com.app.wifiharvest.models.ScanSession
import com.google.gson.Gson
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

object FileManager {
    private const val DIR_NAME = "captures"

    fun listCaptures(context: Context): List<File> {
        val dir = File(context.filesDir, "captures")
        return dir.listFiles()?.sortedByDescending { it.lastModified() } ?: emptyList()
    }

    fun loadCapture(context: Context, file: File): ScanSession? {
        return try {
            val json = file.readText()
            Gson().fromJson(json, ScanSession::class.java)
        } catch (e: Exception) {
            null
        }
    }

    fun saveCapture(context: Context, session: ScanSession) {
        val dir = File(context.filesDir, DIR_NAME)
        if (!dir.exists()) dir.mkdirs()

        val timestamp = SimpleDateFormat("yyyy-MM-dd_HH-mm-ss", Locale.getDefault()).format(Date())
        val file = File(dir, "capture_$timestamp.json")

        file.writeText(Gson().toJson(session))
    }
}
