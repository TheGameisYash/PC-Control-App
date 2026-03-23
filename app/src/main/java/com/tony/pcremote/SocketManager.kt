package com.tony.pcremote

import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress

object SocketManager {
    private const val TAG = "SocketManager"
    private var udpSocket: DatagramSocket? = null
    private var serverAddress: InetAddress? = null
    private const val PORT = 8080

    // ✅ High-performance non-blocking channel for sending
    internal val commandChannel = Channel<ByteArray>(
        capacity = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )

    // ✅ Flow for receiving responses
    private val _responseFlow = MutableSharedFlow<ByteArray>(extraBufferCapacity = 64)
    val responseFlow = _responseFlow.asSharedFlow()

    private var senderJob: Job? = null
    private var receiverJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    suspend fun connect(ip: String): Boolean {
        Log.d(TAG, "Attempting to connect to IP: $ip")
        return withContext(Dispatchers.IO) {
            try {
                disconnect()
                serverAddress = InetAddress.getByName(ip)
                udpSocket = DatagramSocket().apply {
                    broadcast = true // Enable broadcast
                    sendBufferSize = 128 * 1024
                    receiveBufferSize = 128 * 1024
                    soTimeout = 0
                }
                
                startSender()
                startReceiver()
                
                sendRaw(BinaryProtocol.ping())
                Log.d(TAG, "Socket initialized and ping sent to $ip")
                true
            } catch (e: Exception) {
                Log.e(TAG, "Connection failed: ${e.message}", e)
                false
            }
        }
    }

    private fun startSender() {
        senderJob?.cancel()
        senderJob = scope.launch {
            Log.d(TAG, "Sender thread started")
            for (packet in commandChannel) {
                if (!isActive) break
                sendRaw(packet)
            }
            Log.d(TAG, "Sender thread stopped")
        }
    }

    private fun startReceiver() {
        receiverJob?.cancel()
        receiverJob = scope.launch {
            Log.d(TAG, "Receiver thread started")
            val buffer = ByteArray(65535)
            val packet = DatagramPacket(buffer, buffer.size)
            while (isActive) {
                try {
                    val socket = udpSocket ?: break
                    socket.receive(packet)
                    val data = packet.data.copyOfRange(0, packet.length)
                    Log.v(TAG, "Received packet: ${data.size} bytes, type: ${if (data.isNotEmpty()) data[0] else "empty"}")
                    _responseFlow.emit(data)
                } catch (e: Exception) {
                    if (isActive) {
                        Log.e(TAG, "Receiver error: ${e.message}")
                    }
                    break
                }
            }
            Log.d(TAG, "Receiver thread stopped")
        }
    }

    fun sendRaw(data: ByteArray) {
        try {
            val address = serverAddress ?: return
            Log.v(TAG, "Sending packet: ${data.size} bytes, type: ${if (data.isNotEmpty()) data[0] else "empty"}")
            udpSocket?.send(DatagramPacket(data, data.size, address, PORT))
        } catch (e: Exception) {
            Log.e(TAG, "Failed to send packet: ${e.message}")
        }
    }

    fun enqueue(data: ByteArray) {
        commandChannel.trySend(data)
    }

    fun disconnect() {
        Log.d(TAG, "Disconnecting socket...")
        senderJob?.cancel()
        senderJob = null
        receiverJob?.cancel()
        receiverJob = null
        try { udpSocket?.close() } catch (_: Exception) {}
        udpSocket = null
        serverAddress = null
    }

    fun isConnected() = udpSocket != null && !udpSocket!!.isClosed
}
