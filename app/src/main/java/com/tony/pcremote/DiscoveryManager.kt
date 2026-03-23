package com.tony.pcremote

import android.content.Context
import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import android.net.wifi.WifiManager
import android.util.Log
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress

data class DiscoveredDevice(val name: String, val ip: String)

class DiscoveryManager(private val context: Context) {
    private val TAG = "DiscoveryManager"
    private val nsdManager = context.getSystemService(Context.NSD_SERVICE) as NsdManager
    private val SERVICE_TYPE = "_pcremote._udp."
    private val DISCOVERY_PORT = 8080

    private val _discoveredDevices = MutableStateFlow<Set<DiscoveredDevice>>(emptySet())
    val discoveredDevices: StateFlow<Set<DiscoveredDevice>> = _discoveredDevices

    private var discoveryListener: NsdManager.DiscoveryListener? = null
    private var isDiscoveryActive = false
    private var udpDiscoveryJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    @Synchronized
    fun startDiscovery() {
        if (isDiscoveryActive) return
        isDiscoveryActive = true
        _discoveredDevices.value = emptySet()

        startNsdDiscovery()
        startUdpBroadcastDiscovery()
    }

    private fun startNsdDiscovery() {
        val listener = object : NsdManager.DiscoveryListener {
            override fun onDiscoveryStarted(regType: String) { Log.d(TAG, "NSD Started") }
            override fun onServiceFound(service: NsdServiceInfo) {
                if (service.serviceType.contains("_pcremote")) {
                    nsdManager.resolveService(service, object : NsdManager.ResolveListener {
                        override fun onResolveFailed(s: NsdServiceInfo, err: Int) {}
                        override fun onServiceResolved(s: NsdServiceInfo) {
                            val host = s.host?.hostAddress
                            if (host != null) {
                                _discoveredDevices.update { it + DiscoveredDevice(s.serviceName, host) }
                            }
                        }
                    })
                }
            }
            override fun onServiceLost(s: NsdServiceInfo) {
                _discoveredDevices.update { devices -> devices.filterNot { it.name == s.serviceName }.toSet() }
            }
            override fun onDiscoveryStopped(p0: String) {}
            override fun onStartDiscoveryFailed(p0: String, p1: Int) {}
            override fun onStopDiscoveryFailed(p0: String, p1: Int) {}
        }
        discoveryListener = listener
        try {
            nsdManager.discoverServices(SERVICE_TYPE, NsdManager.PROTOCOL_DNS_SD, listener)
        } catch (e: Exception) { Log.e(TAG, "NSD Error", e) }
    }

    private fun startUdpBroadcastDiscovery() {
        udpDiscoveryJob?.cancel()
        udpDiscoveryJob = scope.launch {
            var socket: DatagramSocket? = null
            try {
                socket = DatagramSocket().apply {
                    broadcast = true
                    soTimeout = 2000
                }

                val pingData = byteArrayOf(BinaryProtocol.PING)
                val broadcastAddr = getBroadcastAddress()
                val packet = DatagramPacket(pingData, pingData.size, broadcastAddr, DISCOVERY_PORT)

                // Sender loop
                launch {
                    while (isActive) {
                        try {
                            socket.send(packet)
                            // Also send to universal broadcast as fallback
                            socket.send(DatagramPacket(pingData, pingData.size, InetAddress.getByName("255.255.255.255"), DISCOVERY_PORT))
                        } catch (e: Exception) { Log.e(TAG, "Broadcast send error: ${e.message}") }
                        delay(3000)
                    }
                }

                // Receiver loop
                val buffer = ByteArray(1024)
                while (isActive) {
                    try {
                        val receivePacket = DatagramPacket(buffer, buffer.size)
                        socket.receive(receivePacket)
                        if (receivePacket.length > 0 && buffer[0] == BinaryProtocol.RESP_PONG) {
                            val ip = receivePacket.address?.hostAddress
                            if (ip != null) {
                                Log.d(TAG, "Discovered PC via UDP at $ip")
                                _discoveredDevices.update { it + DiscoveredDevice("PC ($ip)", ip) }
                            }
                        }
                    } catch (e: Exception) { /* Timeout expected */ }
                }
            } catch (e: Exception) {
                Log.e(TAG, "UDP Discovery Error: ${e.message}")
            } finally {
                socket?.close()
            }
        }
    }

    private fun getBroadcastAddress(): InetAddress {
        val wifi = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
        val dhcp = wifi.dhcpInfo
        if (dhcp == null || dhcp.ipAddress == 0) return InetAddress.getByName("255.255.255.255")
        
        val broadcast = (dhcp.ipAddress and dhcp.netmask) or dhcp.netmask.inv()
        val quads = ByteArray(4)
        for (k in 0..3) quads[k] = (broadcast shr (k * 8) and 0xFF).toByte()
        return InetAddress.getByAddress(quads)
    }

    @Synchronized
    fun stopDiscovery() {
        isDiscoveryActive = false
        udpDiscoveryJob?.cancel()
        udpDiscoveryJob = null
        discoveryListener?.let {
            try { nsdManager.stopServiceDiscovery(it) } catch (e: Exception) {}
        }
        discoveryListener = null
    }
}
