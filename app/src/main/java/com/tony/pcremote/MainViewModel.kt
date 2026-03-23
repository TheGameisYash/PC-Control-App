package com.tony.pcremote

import android.app.Application
import android.content.Context
import android.net.wifi.WifiManager
import androidx.compose.ui.graphics.ImageBitmap
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.datastore.preferences.core.edit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import org.json.JSONArray

// ✅ NEW IMPORTS
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject

// ✅ Announcements data class
data class Announcement(
    val title: String,
    val message: String,
    val type: String,  // "info", "warning", "critical"
    val expiresAt: String?
)

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val _isConnected   = MutableStateFlow(false)
    val isConnected: StateFlow<Boolean> = _isConnected

    private val _statusMessage = MutableStateFlow("Enter your PC's IP address")
    val statusMessage: StateFlow<String> = _statusMessage

    private val _serverIp      = MutableStateFlow("")
    val serverIp: StateFlow<String> = _serverIp

    // Screen frame — null when stream is inactive
    private val _screenFrame   = MutableStateFlow<ImageBitmap?>(null)
    val screenFrame: StateFlow<ImageBitmap?> = _screenFrame

    // ✅ NEW: Announcements state
    private val _announcements = MutableStateFlow<List<Announcement>>(emptyList())
    val announcements: StateFlow<List<Announcement>> = _announcements

    // PC resolution (still used for stream, not mouse now)
    val pcResolution: StateFlow<Pair<Int, Int>> = ScreenStreamManager.pcResolution

    private var screenStreamJob: Job? = null

    private val wifiManager =
        application.getSystemService(Context.WIFI_SERVICE) as WifiManager
    private var wifiLock: WifiManager.WifiLock? = null

    init {
        // ✅ Command sender loop
        viewModelScope.launch(Dispatchers.IO) {
            for (packet in SocketManager.commandChannel) {
                SocketManager.sendRaw(packet)
            }
        }

        // ✅ License re-validation on app launch
        viewModelScope.launch {
            val prefs  = UserPreferences(getApplication())
            val key    = prefs.licenseKey.first()
            val user   = prefs.username.first()
            val isPrem = prefs.isPremium.first()

            if (isPrem && key.isNotEmpty() && user.isNotEmpty()) {
                val valid = LicenseManager.validateLicense(key, user)

                if (!valid) {
                    getApplication<Application>().dataStore.edit {
                        it[UserPreferences.KEY_IS_PREMIUM] = false
                    }
                }
            }
        }
    }

    // ✅ NEW: Fetch announcements
    fun fetchAnnouncements() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val client  = OkHttpClient()
                val request = Request.Builder()
                    .url("https://license-system-pi.vercel.app/api/announcements?softwareId=pc-remote-mn009le4")
                    .get()
                    .build()

                val response = client.newCall(request).execute()
                val body     = response.body?.string() ?: return@launch
                val json     = JSONObject(body)
                val arr      = json.optJSONArray("announcements") ?: return@launch

                val list = (0 until arr.length()).map { i ->
                    val obj = arr.getJSONObject(i)
                    Announcement(
                        title     = obj.optString("title"),
                        message   = obj.optString("message"),
                        type      = obj.optString("type", "info"),
                        expiresAt = obj.optString("expiresAt")
                    )
                }

                _announcements.value = list

            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun connect(ip: String) {
        _statusMessage.value = "Connecting..."
        viewModelScope.launch {
            val result = SocketManager.connect(ip)
            _isConnected.value = result

            if (result) {
                _serverIp.value = ip
                _statusMessage.value = "Connected to $ip ✅"
                acquireWifiLock()
            } else {
                _statusMessage.value = "❌ Failed. Check IP & make sure server is running."
            }
        }
    }

    fun send(data: ByteArray) = SocketManager.enqueue(data)

    // ── Screen stream ──
    fun startScreenStream() {
        val ip = _serverIp.value
        if (ip.isBlank() || screenStreamJob?.isActive == true) return

        screenStreamJob = viewModelScope.launch {
            try {
                ScreenStreamManager.frameFlow(ip).collect { frame ->
                    _screenFrame.value = frame
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun stopScreenStream() {
        screenStreamJob?.cancel()
        screenStreamJob = null
        _screenFrame.value = null
    }

    // ✅ Absolute mouse mapping
    fun sendAbsMouseMove(
        touchX: Float,
        touchY: Float,
        containerW: Int,
        containerH: Int
    ) {
        if (containerW == 0 || containerH == 0) return

        val normX = ((touchX / containerW) * 65535)
            .toInt()
            .coerceIn(0, 65535)

        val normY = ((touchY / containerH) * 65535)
            .toInt()
            .coerceIn(0, 65535)

        send(BinaryProtocol.mouseMoveAbs(normX, normY))
    }

    fun disconnect() {
        stopScreenStream()
        SocketManager.disconnect()
        _isConnected.value = false
        _serverIp.value = ""
        _statusMessage.value = "Disconnected"
        releaseWifiLock()
    }

    private fun acquireWifiLock() {
        try {
            wifiLock = wifiManager.createWifiLock(
                WifiManager.WIFI_MODE_FULL_LOW_LATENCY,
                "PCRemoteLock"
            ).also { if (!it.isHeld) it.acquire() }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun releaseWifiLock() {
        try {
            wifiLock?.let { if (it.isHeld) it.release() }
        } catch (_: Exception) {}
    }

    override fun onCleared() {
        super.onCleared()
        stopScreenStream()
        SocketManager.disconnect()
        releaseWifiLock()
    }
}