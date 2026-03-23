package com.tony.pcremote

import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL

// Response models matching your server's JSON
data class LicenseResponse(
    @SerializedName("success") val success: Boolean = false,
    @SerializedName("code")    val code:    String  = "",
    @SerializedName("message") val message: String  = ""
)

object LicenseManager {

    private const val BASE_URL = "https://license-system-pi.vercel.app"

    // Called when user activates license for the first time
    // username is used as HWID — binds license to account, not hardware
    suspend fun activateLicense(licenseKey: String, username: String): LicenseResponse =
        withContext(Dispatchers.IO) {
            try {
                val url  = URL("$BASE_URL/api/register")
                val conn = (url.openConnection() as HttpURLConnection).apply {
                    requestMethod  = "POST"
                    connectTimeout = 8000
                    readTimeout    = 8000
                    setRequestProperty("Content-Type", "application/json")
                    doOutput = true
                }

                val body = Gson().toJson(mapOf(
                    "license"     to licenseKey.trim().uppercase(),
                    "hwid" to username.trim().lowercase().replace(Regex("[^a-z0-9]"), ""),
                    "device_name" to "PCRemote Android",
                    "device_info" to "Android App"
                ))

                conn.outputStream.use { it.write(body.toByteArray()) }

                val responseCode = conn.responseCode
                val stream = if (responseCode in 200..299)
                    conn.inputStream else conn.errorStream

                val json = stream.bufferedReader().readText()
                conn.disconnect()

                Gson().fromJson(json, LicenseResponse::class.java)
            } catch (e: Exception) {
                LicenseResponse(false, "ERROR", "Network error: ${e.message}")
            }
        }

    // Called on every app launch to verify license is still valid
    suspend fun validateLicense(licenseKey: String, username: String): Boolean =
        withContext(Dispatchers.IO) {
            try {
                val hwid = username.trim().lowercase().replace(Regex("[^a-z0-9]"), "")
                val url  = URL("$BASE_URL/api/validate?license=${licenseKey.trim().uppercase()}&hwid=$hwid")
                val conn = (url.openConnection() as HttpURLConnection).apply {
                    requestMethod  = "GET"
                    connectTimeout = 8000
                    readTimeout    = 8000
                }

                val responseCode = conn.responseCode
                val json = conn.inputStream.bufferedReader().readText()
                conn.disconnect()

                if (responseCode == 200) {
                    val resp = Gson().fromJson(json, LicenseResponse::class.java)
                    resp.success && resp.code == "VALID"
                } else false
            } catch (_: Exception) {
                // If network fails, trust local cached premium status
                true
            }
        }

    // Check license info without HWID (for display purposes)
    suspend fun getLicenseInfo(licenseKey: String): LicenseResponse =
        withContext(Dispatchers.IO) {
            try {
                val url  = URL("$BASE_URL/api/license-info?license=${licenseKey.trim().uppercase()}")
                val conn = (url.openConnection() as HttpURLConnection).apply {
                    requestMethod  = "GET"
                    connectTimeout = 8000
                    readTimeout    = 8000
                }
                val json = conn.inputStream.bufferedReader().readText()
                conn.disconnect()
                Gson().fromJson(json, LicenseResponse::class.java)
            } catch (e: Exception) {
                LicenseResponse(false, "ERROR", e.message ?: "Unknown error")
            }
        }
}
