package com.tony.pcremote

import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody

data class LicenseResponse(
    @SerializedName("success") val success: Boolean = false,
    @SerializedName("code")    val code:    String  = "",
    @SerializedName("message") val message: String  = ""
)

object LicenseManager {
    private const val BASE_URL = "https://license-system-pi.vercel.app"
    private const val GRACE_PERIOD_MS = 7 * 24 * 60 * 60 * 1000L 
    private val client = OkHttpClient()
    private val gson = Gson()
    private val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaType()

    suspend fun activateLicense(licenseKey: String, username: String): LicenseResponse =
        withContext(Dispatchers.IO) {
            try {
                val hwid = username.trim().lowercase().replace(Regex("[^a-z0-9]"), "")
                val bodyMap = mapOf(
                    "license"     to licenseKey.trim().uppercase(),
                    "hwid"        to hwid,
                    "device_name" to "PCRemote Android",
                    "device_info" to "Android App"
                )
                val requestBody = gson.toJson(bodyMap).toRequestBody(JSON_MEDIA_TYPE)
                
                val request = Request.Builder()
                    .url("$BASE_URL/api/register")
                    .post(requestBody)
                    .build()

                client.newCall(request).execute().use { response ->
                    val json = response.body?.string() ?: return@withContext LicenseResponse(false, "ERROR", "Empty response")
                    gson.fromJson(json, LicenseResponse::class.java)
                }
            } catch (e: Exception) {
                LicenseResponse(false, "ERROR", "Network error: ${e.message}")
            }
        }

    suspend fun validateLicense(licenseKey: String, username: String, prefs: UserPreferences): Boolean =
        withContext(Dispatchers.IO) {
            try {
                val hwid = username.trim().lowercase().replace(Regex("[^a-z0-9]"), "")
                val url = "$BASE_URL/api/validate?license=${licenseKey.trim().uppercase()}&hwid=$hwid"
                
                val request = Request.Builder()
                    .url(url)
                    .get()
                    .build()

                client.newCall(request).execute().use { response ->
                    if (response.isSuccessful) {
                        val json = response.body?.string() ?: return@use false
                        val resp = gson.fromJson(json, LicenseResponse::class.java)
                        val isValid = resp.success && resp.code == "VALID"
                        if (isValid) {
                            prefs.updateValidationTime()
                        }
                        isValid
                    } else {
                        false
                    }
                }
            } catch (e: Exception) {
                val lastValid = prefs.lastValidated.first()
                val now = System.currentTimeMillis()
                if (lastValid == 0L || (now - lastValid) > GRACE_PERIOD_MS) {
                    false
                } else {
                    true
                }
            }
        }

    suspend fun getLicenseInfo(licenseKey: String): LicenseResponse =
        withContext(Dispatchers.IO) {
            try {
                val request = Request.Builder()
                    .url("$BASE_URL/api/license-info?license=${licenseKey.trim().uppercase()}")
                    .get()
                    .build()

                client.newCall(request).execute().use { response ->
                    val json = response.body?.string() ?: return@withContext LicenseResponse(false, "ERROR", "Empty response")
                    gson.fromJson(json, LicenseResponse::class.java)
                }
            } catch (e: Exception) {
                LicenseResponse(false, "ERROR", e.message ?: "Unknown error")
            }
        }
}
