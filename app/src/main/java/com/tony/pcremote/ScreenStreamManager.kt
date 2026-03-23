package com.tony.pcremote

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import java.io.BufferedInputStream
import java.io.DataInputStream
import java.net.Socket

object ScreenStreamManager {
    private const val SCREEN_PORT = 8081

    // PC resolution — received from server at stream start
    private val _pcResolution = MutableStateFlow(Pair(1920, 1080))
    val pcResolution: StateFlow<Pair<Int, Int>> = _pcResolution

    fun frameFlow(ip: String): Flow<ImageBitmap> = flow {
        Socket(ip, SCREEN_PORT).use { socket ->
            socket.soTimeout = 0
            socket.receiveBufferSize = 2 * 1024 * 1024
            val stream = DataInputStream(BufferedInputStream(socket.getInputStream(), 65536))

            // First 8 bytes = PC screen resolution sent by server
            val pcW = stream.readInt()
            val pcH = stream.readInt()
            _pcResolution.value = Pair(pcW, pcH)

            // RGB_565 = half memory vs ARGB_8888, faster decode for streaming
            val options = BitmapFactory.Options().apply {
                inPreferredConfig = Bitmap.Config.RGB_565
            }

            while (true) {
                val length = stream.readInt()
                if (length <= 0 || length > 5_000_000) continue
                val data = ByteArray(length)
                stream.readFully(data)
                val bmp = BitmapFactory.decodeByteArray(data, 0, length, options) ?: continue
                emit(bmp.asImageBitmap())
            }
        }
    }.flowOn(Dispatchers.IO)
}
