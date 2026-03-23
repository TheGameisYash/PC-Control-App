package com.tony.pcremote

import android.app.Application
import android.content.Context
import android.net.wifi.WifiManager
import android.util.Log
import androidx.compose.ui.graphics.ImageBitmap
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.roundToInt

// ✅ Announcements data class
data class Announcement(
    val title: String,
    val message: String,
    val type: String,
    val expiresAt: String?
)

class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val TAG = "MainViewModel"

    private val _isConnected   = MutableStateFlow(false)
    val isConnected: StateFlow<Boolean> = _isConnected

    private val _statusMessage = MutableStateFlow("Enter your PC's IP address")
    val statusMessage: StateFlow<String> = _statusMessage

    private val _serverIp      = MutableStateFlow("")
    val serverIp: StateFlow<String> = _serverIp

    private val _pcName        = MutableStateFlow("Remote PC")
    val pcName: StateFlow<String> = _pcName

    private val _screenFrame   = MutableStateFlow<ImageBitmap?>(null)
    val screenFrame: StateFlow<ImageBitmap?> = _screenFrame

    private val _announcements = MutableStateFlow<List<Announcement>>(emptyList())
    val announcements: StateFlow<List<Announcement>> = _announcements

    // ✅ UTILITIES STATE
    private val _processes = MutableStateFlow<List<ProcessInfo>>(emptyList())
    val processes: StateFlow<List<ProcessInfo>> = _processes

    private val _files = MutableStateFlow<List<FileItem>>(emptyList())
    val files: StateFlow<List<FileItem>> = _files

    private val _currentFilePath = MutableStateFlow("C:\\")
    val currentFilePath: StateFlow<String> = _currentFilePath

    private val _volumeDevices = MutableStateFlow<List<VolumeDeviceInfo>>(emptyList())
    val volumeDevices: StateFlow<List<VolumeDeviceInfo>> = _volumeDevices

    private val _terminalOutput = MutableStateFlow("PCRemote Terminal Ready\n")
    val terminalOutput: StateFlow<String> = _terminalOutput

    // ✅ Discovery
    private val discoveryManager = DiscoveryManager(application)
    val discoveredDevices: StateFlow<Set<DiscoveredDevice>> = discoveryManager.discoveredDevices

    val pcResolution: StateFlow<Pair<Int, Int>> = ScreenStreamManager.pcResolution

    private var screenStreamJob: Job? = null
    private val userPrefs = UserPreferences(application)

    private val okHttpClient = OkHttpClient()

    private val wifiManager =
        application.getSystemService(Context.WIFI_SERVICE) as WifiManager
    private var wifiLock: WifiManager.WifiLock? = null

    init {
        Log.d(TAG, "ViewModel initialized")
        
        // ✅ License validation
        viewModelScope.launch {
            try {
                val key    = userPrefs.licenseKey.first()
                val user   = userPrefs.username.first()
                val isPrem = userPrefs.isPremium.first()
                Log.d(TAG, "Checking license: User=$user, Premium=$isPrem, Key=$key")

                if (isPrem && key.isNotEmpty() && user.isNotEmpty()) {
                    val valid = LicenseManager.validateLicense(key, user, userPrefs)
                    Log.d(TAG, "License validation result: $valid")
                    if (!valid) {
                        userPrefs.setPremium(false)
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "License init error: ${e.message}")
            }
        }

        // ✅ Listen for responses from SocketManager
        viewModelScope.launch(Dispatchers.IO) {
            SocketManager.responseFlow.collect { data ->
                handleResponse(data)
            }
        }
    }

    private fun handleResponse(data: ByteArray) {
        if (data.isEmpty()) return
        val type = data[0]
        Log.d(TAG, "Handling response type: ${type.toInt() and 0xFF}, size: ${data.size}")

        try {
            when (type) {
                BinaryProtocol.RESP_PROCESSES -> {
                    val jsonStr = String(data, 1, data.size - 1, Charsets.UTF_8)
                    val arr = JSONArray(jsonStr)
                    val list = mutableListOf<ProcessInfo>()
                    for (i in 0 until arr.length()) {
                        val obj = arr.getJSONObject(i)
                        list.add(ProcessInfo(
                            pid = obj.getInt("id"),
                            name = obj.getString("name"),
                            cpu = 0f, // Server doesn't send CPU currently
                            memory = obj.optLong("mem", 0L)
                        ))
                    }
                    _processes.value = list
                    Log.d(TAG, "Processes updated: ${list.size} items")
                }
                BinaryProtocol.RESP_FILES -> {
                    val jsonStr = String(data, 1, data.size - 1, Charsets.UTF_8)
                    val arr = JSONArray(jsonStr)
                    val list = mutableListOf<FileItem>()
                    for (i in 0 until arr.length()) {
                        val f = arr.getJSONObject(i)
                        list.add(FileItem(
                            name = f.getString("name"),
                            isDirectory = f.getString("type") != "file",
                            size = f.optLong("size", 0L),
                            path = f.getString("path")
                        ))
                    }
                    _files.value = list
                    Log.d(TAG, "Files updated: ${list.size} items")
                }
                BinaryProtocol.RESP_VOLUME -> {
                    if (data.size >= 3) {
                        val vol = data[1].toInt() and 0xFF
                        val muted = data[2].toInt() == 1
                        val list = listOf(VolumeDeviceInfo("default", "System Volume", vol, muted))
                        _volumeDevices.value = list
                        Log.d(TAG, "Volume updated: $vol%, muted: $muted")
                    }
                }
                BinaryProtocol.RESP_TERMINAL -> {
                    val output = String(data, 1, data.size - 1, Charsets.UTF_8)
                    _terminalOutput.value += output + "\n"
                    Log.v(TAG, "Terminal output received: $output")
                }
                BinaryProtocol.RESP_ERROR -> {
                    val error = String(data, 1, data.size - 1, Charsets.UTF_8)
                    _terminalOutput.value += "[ERROR] $error\n"
                    Log.e(TAG, "Server Error: $error")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing response: ${e.message}")
        }
    }

    fun startDiscovery() {
        Log.i(TAG, "UI requested discovery start")
        discoveryManager.startDiscovery()
    }
    
    fun stopDiscovery() {
        Log.i(TAG, "UI requested discovery stop")
        discoveryManager.stopDiscovery()
    }

    fun connect(ip: String, name: String = "Remote PC") {
        Log.i(TAG, "Attempting connection to $ip ($name)")
        _statusMessage.value = "Connecting to $name..."
        viewModelScope.launch {
            val result = SocketManager.connect(ip)
            _isConnected.value = result

            if (result) {
                Log.i(TAG, "Connected successfully to $ip")
                _serverIp.value = ip
                _pcName.value = name
                _statusMessage.value = "Connected to $name ✅"
                acquireWifiLock()
                stopDiscovery()
            } else {
                Log.e(TAG, "Connection failed to $ip")
                _statusMessage.value = "❌ Failed to connect to $name"
            }
        }
    }

    fun send(data: ByteArray) {
        if (!_isConnected.value) {
            Log.w(TAG, "Attempted to send data while disconnected")
        }
        SocketManager.enqueue(data)
    }

    // ✅ UTILITIES ACTIONS
    fun refreshProcesses() {
        Log.d(TAG, "Refreshing processes...")
        send(BinaryProtocol.reqProcesses())
    }

    fun killProcess(pid: Int) {
        Log.w(TAG, "Killing process PID: $pid")
        send(BinaryProtocol.killProcess(pid))
        viewModelScope.launch {
            kotlinx.coroutines.delay(800)
            refreshProcesses()
        }
    }

    fun requestFiles(path: String) {
        Log.d(TAG, "Requesting files for path: $path")
        _currentFilePath.value = path
        send(BinaryProtocol.reqFiles(path))
    }

    fun navigateUp() {
        val current = _currentFilePath.value
        Log.d(TAG, "Navigating up from: $current")
        if (current.isEmpty()) return
        
        val parent = if (current.endsWith(":\\")) {
            "" // Go back to drives
        } else {
            val trimmed = current.trimEnd('\\')
            val idx = trimmed.lastIndexOf('\\')
            if (idx == -1) "" else trimmed.substring(0, idx + 1)
        }
        requestFiles(parent)
    }

    fun refreshVolumes() {
        Log.d(TAG, "Refreshing volumes...")
        send(BinaryProtocol.reqVolume())
    }

    fun setVolume(deviceId: String, level: Int) {
        Log.v(TAG, "Setting volume to $level")
        send(BinaryProtocol.setVolume(level))
    }
    
    fun toggleMute() {
        Log.v(TAG, "Toggling mute")
        send(BinaryProtocol.setMute(2)) // 2 = toggle in your server
    }

    fun runTerminalCommand(cmd: String) {
        Log.i(TAG, "Terminal command: $cmd")
        _terminalOutput.value += "> $cmd\n"
        send(BinaryProtocol.terminalCmd(cmd))
    }

    fun startScreenStream() {
        val ip = _serverIp.value
        Log.i(TAG, "Starting screen stream to $ip")
        if (ip.isBlank() || screenStreamJob?.isActive == true) return

        screenStreamJob = viewModelScope.launch {
            try {
                ScreenStreamManager.frameFlow(ip).collect { frame ->
                    _screenFrame.value = frame
                }
            } catch (e: Exception) {
                Log.e(TAG, "Screen stream error: ${e.message}")
            }
        }
    }

    fun stopScreenStream() {
        Log.i(TAG, "Stopping screen stream")
        screenStreamJob?.cancel()
        screenStreamJob = null
        _screenFrame.value = null
    }

    fun sendAbsMouseMove(touchX: Float, touchY: Float, viewW: Int, viewH: Int) {
        if (viewW <= 0 || viewH <= 0) return
        val (pcW, pcH) = pcResolution.value
        val pcX = (touchX / viewW * pcW).roundToInt()
        val pcY = (touchY / viewH * pcH).roundToInt()
        send(BinaryProtocol.mouseMoveAbs(pcX, pcY))
    }

    fun shutdown() {
        send(BinaryProtocol.shutdown())
    }

    fun restart() {
        send(BinaryProtocol.restart())
    }

    fun sleep() {
        send(BinaryProtocol.sleep())
    }

    fun disconnect() {
        Log.i(TAG, "User requested disconnect")
        stopScreenStream()
        SocketManager.disconnect()
        _isConnected.value = false
        _serverIp.value = ""
        _pcName.value = "Remote PC"
        _statusMessage.value = "Disconnected"
        releaseWifiLock()
    }

    private fun acquireWifiLock() {
        try {
            wifiLock = wifiManager.createWifiLock(
                WifiManager.WIFI_MODE_FULL_LOW_LATENCY,
                "PCRemoteLock"
            ).also { if (!it.isHeld) it.acquire() }
            Log.d(TAG, "WiFi lock acquired")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to acquire WiFi lock: ${e.message}")
        }
    }

    private fun releaseWifiLock() {
        try {
            wifiLock?.let { if (it.isHeld) it.release() }
            Log.d(TAG, "WiFi lock released")
        } catch (_: Exception) {}
    }

    override fun onCleared() {
        super.onCleared()
        Log.d(TAG, "ViewModel onCleared called")
        disconnect()
        stopDiscovery()
    }

    fun fetchAnnouncements() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                Log.d(TAG, "Fetching announcements...")
                val request = Request.Builder()
                    .url("https://license-system-pi.vercel.app/api/announcements?softwareId=pc-remote-mn009le4")
                    .get()
                    .build()

                okHttpClient.newCall(request).execute().use { response ->
                    val body = response.body?.string() ?: return@use
                    val json = JSONObject(body)
                    val arr = json.optJSONArray("announcements") ?: return@use

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
                    Log.d(TAG, "Fetched ${list.size} announcements")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Announcements fetch failed: ${e.message}")
            }
        }
    }
}
