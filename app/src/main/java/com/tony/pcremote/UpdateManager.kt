package com.tony.pcremote

import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.content.FileProvider
import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.net.URL

// Model for GitHub API Response
data class GithubRelease(
    @SerializedName("tag_name") val tagName: String,
    @SerializedName("body") val body: String,
    @SerializedName("assets") val assets: List<GithubAsset>
)

data class GithubAsset(
    @SerializedName("name") val name: String,
    @SerializedName("browser_download_url") val downloadUrl: String
)

data class UpdateInfo(
    val version_code: Int,
    val version_name: String,
    val apk_url:      String,
    val changelog:    String
)

object UpdateManager {
    private const val TAG = "UpdateManager"
    
    // ⚠️ Uses GitHub API to find the latest release
    private const val GITHUB_API_URL = "https://api.github.com/repos/yash2/PCRemote/releases/latest"
    private val client = OkHttpClient()

    suspend fun checkForUpdate(currentVersionCode: Int): UpdateInfo? =
        withContext(Dispatchers.IO) {
            try {
                val request = Request.Builder()
                    .url(GITHUB_API_URL)
                    .header("Accept", "application/vnd.github.v3+json")
                    .build()
                    
                client.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) return@use null
                    
                    val json = response.body?.string() ?: return@use null
                    val release = Gson().fromJson(json, GithubRelease::class.java)
                    
                    // Extract version code from tag (e.g., "v2" -> 2)
                    val remoteVersionCode = release.tagName.replace(Regex("[^0-9]"), "").toIntOrNull() ?: 0
                    
                    // Find the APK in assets
                    val apkAsset = release.assets.find { it.name.endsWith(".apk") }
                    
                    if (remoteVersionCode > currentVersionCode && apkAsset != null) {
                        UpdateInfo(
                            version_code = remoteVersionCode,
                            version_name = release.tagName,
                            apk_url      = apkAsset.downloadUrl,
                            changelog    = release.body
                        )
                    } else null
                }
            } catch (e: Exception) {
                Log.e(TAG, "Update check failed: ${e.message}")
                null
            }
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
        } catch (e: Exception) { 
            Log.e(TAG, "Download failed: ${e.message}")
            null 
        }
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
