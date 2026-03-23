package com.tony.pcremote

import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL

// ── Response models ───────────────────────────────────────────────────────────

data class AuthResponse(
    @SerializedName("success") val success: Boolean   = false,
    @SerializedName("code")    val code:    String    = "",
    @SerializedName("message") val message: String    = "",
    @SerializedName("data")    val data:    AuthData? = null
)

data class AuthData(
    @SerializedName("username")       val username:       String  = "",
    @SerializedName("email")          val email:          String  = "",
    @SerializedName("softwareId")     val softwareId:     String  = "",
    @SerializedName("licenseKey")     val licenseKey:     String  = "",
    @SerializedName("isPremium")      val isPremium:      Boolean = false,
    @SerializedName("licenseStatus")  val licenseStatus:  String  = "unknown",
    @SerializedName("licenseWarning") val licenseWarning: String? = null,
    @SerializedName("expiresAt")      val expiresAt:      String? = null,
    @SerializedName("activatedAt")    val activatedAt:    String? = null,
    @SerializedName("lastLogin")      val lastLogin:      String? = null
)

// ── License status helpers ────────────────────────────────────────────────────

enum class LicenseStatus {
    ACTIVE,
    EXPIRED,
    BANNED,
    NO_LICENSE,
    UNKNOWN;

    companion object {
        fun from(raw: String?): LicenseStatus = when (raw) {
            "active"          -> ACTIVE
            "LICENSE_EXPIRED" -> EXPIRED
            "LICENSE_BANNED"  -> BANNED
            "NO_LICENSE"      -> NO_LICENSE
            else              -> UNKNOWN
        }
    }
}

// Convenience — call after login to get a user-friendly status message
fun AuthData.licenseStatusMessage(): String? = when (licenseWarning) {
    "LICENSE_BANNED"  -> "⚠️ Your license has been banned. Contact support."
    "LICENSE_EXPIRED" -> "⚠️ Your license has expired. Please renew."
    "NO_LICENSE"      -> "⚠️ No license linked to your account."
    else              -> null   // null = no warning needed
}

// ── AuthManager ───────────────────────────────────────────────────────────────

object AuthManager {

    private const val BASE_URL    = "https://license-system-pi.vercel.app"
    const val SOFTWARE_ID         = "pc-remote-mn009le4"
    private const val TIMEOUT_MS  = 12000

    // ── Register ──────────────────────────────────────────────────────────────
    suspend fun register(
        username:   String,
        password:   String,
        licenseKey: String,
        email:      String = ""
    ): AuthResponse = withContext(Dispatchers.IO) {
        post(
            "$BASE_URL/api/users/register",
            mapOf(
                "username"    to username.trim().lowercase(),
                "password"    to password,
                "license_key" to licenseKey.trim().uppercase(),
                "email"       to email.trim(),
                "software_id" to SOFTWARE_ID
            )
        )
    }

    // ── Login ─────────────────────────────────────────────────────────────────
    suspend fun login(
        username: String,
        password: String
    ): AuthResponse = withContext(Dispatchers.IO) {
        post(
            "$BASE_URL/api/users/login",
            mapOf(
                "username"    to username.trim().lowercase(),
                "password"    to password,
                "software_id" to SOFTWARE_ID
            )
        )
    }

    // ── HTTP POST ─────────────────────────────────────────────────────────────
    private fun post(urlStr: String, body: Map<String, String>): AuthResponse {
        var conn: HttpURLConnection? = null
        return try {
            conn = (URL(urlStr).openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                connectTimeout = TIMEOUT_MS
                readTimeout    = TIMEOUT_MS
                doOutput = true
                doInput  = true
                setRequestProperty("Content-Type", "application/json; charset=UTF-8")
                setRequestProperty("Accept",       "application/json")
                setRequestProperty("X-Software-Id", SOFTWARE_ID)
            }

            val bodyBytes = Gson().toJson(body).toByteArray(Charsets.UTF_8)
            conn.setFixedLengthStreamingMode(bodyBytes.size)
            conn.outputStream.use { it.write(bodyBytes) }

            val httpCode = conn.responseCode
            val json = try {
                val stream = if (httpCode in 200..299) conn.inputStream
                else conn.errorStream ?: conn.inputStream
                BufferedReader(InputStreamReader(stream, Charsets.UTF_8)).use { it.readText() }
            } catch (e: Exception) {
                // Can't read body — map HTTP code to known error
                return httpCodeToResponse(httpCode)
            }

            Gson().fromJson(json, AuthResponse::class.java)
                ?: AuthResponse(false, "PARSE_ERROR", "Could not parse server response")

        } catch (e: java.net.SocketTimeoutException) {
            AuthResponse(false, "TIMEOUT",
                "Connection timed out. Check your internet and try again.")
        } catch (e: java.net.UnknownHostException) {
            AuthResponse(false, "NO_INTERNET",
                "Cannot reach server. Check your internet connection.")
        } catch (e: javax.net.ssl.SSLException) {
            AuthResponse(false, "SSL_ERROR",
                "Secure connection failed. Try again.")
        } catch (e: Exception) {
            AuthResponse(false, "NETWORK_ERROR",
                "Network error: ${e.message?.take(80)}")
        } finally {
            conn?.disconnect()
        }
    }

    private fun httpCodeToResponse(code: Int) = when (code) {
        400  -> AuthResponse(false, "MISSING_FIELDS",      "Invalid request data")
        401  -> AuthResponse(false, "INVALID_CREDENTIALS", "Invalid username or password")
        403  -> AuthResponse(false, "FORBIDDEN",           "Access denied")
        404  -> AuthResponse(false, "LICENSE_NOT_FOUND",   "License key not found")
        409  -> AuthResponse(false, "CONFLICT",            "Username or license already in use")
        429  -> AuthResponse(false, "RATE_LIMITED",        "Too many attempts. Wait 1 minute.")
        500  -> AuthResponse(false, "SERVER_ERROR",        "Server error. Try again later.")
        502, 503, 504
            -> AuthResponse(false, "SERVER_DOWN",         "Server is temporarily unavailable.")
        else -> AuthResponse(false, "HTTP_$code",          "Request failed (HTTP $code)")
    }
}
