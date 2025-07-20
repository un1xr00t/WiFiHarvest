package com.app.wifiharvest

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.core.content.FileProvider
import java.io.File

object ExportHelper {

    fun exportNetworksToCSV(context: Context, networks: List<WifiNetwork>): File? {
        return try {
            val file = File(
                context.getExternalFilesDir(null),
                "WiFiNetworks_${System.currentTimeMillis()}.csv"
            )
            file.bufferedWriter().use { out ->
                out.write("SSID,BSSID,Latitude,Longitude\n")
                networks.forEach { net ->
                    out.write("${net.ssid},${net.bssid},${net.lat},${net.lng}\n")
                }
            }
            Toast.makeText(context, "CSV exported successfully", Toast.LENGTH_SHORT).show()
            file
        } catch (e: Exception) {
            Toast.makeText(context, "Failed to export CSV: ${e.message}", Toast.LENGTH_LONG).show()
            null
        }
    }

    fun shareCSV(context: Context, file: File) {
        try {
            val uri: Uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.provider",
                file
            )

            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "text/csv"
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }

            context.startActivity(Intent.createChooser(intent, "Share Wi-Fi CSV"))
            Toast.makeText(context, "Opening share sheet...", Toast.LENGTH_SHORT).show()

        } catch (e: Exception) {
            Toast.makeText(context, "Failed to share file: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }
}
