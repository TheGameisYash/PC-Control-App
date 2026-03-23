package com.tony.pcremote

import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress

object SocketManager {
    private var udpSocket: DatagramSocket? = null
    private var serverAddress: InetAddress? = null
    private const val PORT = 8080

    // Typed channel now carries ByteArray — no serialization at all
    val commandChannel = Channel<ByteArray>(
        capacity = 128,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )

    suspend fun connect(ip: String): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                disconnect()
                serverAddress = InetAddress.getByName(ip)
                udpSocket = DatagramSocket().apply {
                    sendBufferSize = 65536
                    soTimeout = 0
                }
                sendRaw(BinaryProtocol.ping())
                true
            } catch (e: Exception) {
                e.printStackTrace()
                false
            }
        }
    }

    // Called from Channel consumer — already on IO thread
    fun sendRaw(data: ByteArray) {
        try {
            udpSocket?.send(DatagramPacket(data, data.size, serverAddress, PORT))
        } catch (_: Exception) {}
    }

    // Non-blocking enqueue from UI thread
    fun enqueue(data: ByteArray) {
        commandChannel.trySend(data)
    }

    fun disconnect() {
        try { udpSocket?.close() } catch (_: Exception) {}
        udpSocket = null
        serverAddress = null
    }

    fun isConnected() = udpSocket?.isClosed == false && udpSocket != null
}
