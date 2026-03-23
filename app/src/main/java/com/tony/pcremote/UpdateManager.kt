package com.tony.pcremote

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.net.URL

data class UpdateInfo(
    val version_code: Int    = 0,
    val version_name: String = "",
    val apk_url:      String = "",
    val changelog:    String = ""
)

object UpdateManager {

    // ⚠️ Replace with your actual GitHub repo raw URL
    private const val VERSION_URL =
        "https://raw.githubusercontent.com/YOURUSERNAME/PCRemote/main/version.json"

    suspend fun checkForUpdate(currentVersionCode: Int): UpdateInfo? =
        withContext(Dispatchers.IO) {
            try {
                val json = URL(VERSION_URL).readText()
                val info = Gson().fromJson(json, UpdateInfo::class.java)
                if (info.version_code > currentVersionCode) info else null
            } catch (_: Exception) { null }
        }

    suspend fun downloadApk(
        context: Context,
        apkUrl: String,
        onProgress: (Int) -> Unit
    ): File? = withContext(Dispatchers.IO) {
        try {
            val conn = URL(apkUrl).openConnection().also { it.connect() }
            val total = conn.contentLength
            val dir   = File(context.cacheDir, "updates").also { it.mkdirs() }
            val file  = File(dir, "update.apk")

            conn.getInputStream().use { input ->
                file.outputStream().use { output ->
                    val buffer = ByteArray(8192)
                    var downloaded = 0
                    var read: Int
                    while (input.read(buffer).also { read = it } != -1) {
                        output.write(buffer, 0, read)
                        downloaded += read
                        if (total > 0) onProgress(downloaded * 100 / total)
                    }
                }
            }
            file
        } catch (_: Exception) { null }
    }

    fun installApk(context: Context, file: File) {
        val uri = FileProvider.getUriForFile(
            context, "${context.packageName}.provider", file
        )
        context.startActivity(
            Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "application/vnd.android.package-archive")
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                        Intent.FLAG_GRANT_READ_URI_PERMISSION
            }
        )
    }
}
