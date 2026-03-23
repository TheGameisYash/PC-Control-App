package com.tony.pcremote

import java.nio.ByteBuffer
import java.nio.ByteOrder

object BinaryProtocol {
    // Mouse & Basic (Matches Server)
    const val MOUSE_MOVE:     Byte = 0x01
    const val MOUSE_LEFT:     Byte = 0x02
    const val MOUSE_RIGHT:    Byte = 0x03
    const val MOUSE_MIDDLE:   Byte = 0x04
    const val MOUSE_DOUBLE:   Byte = 0x05
    const val SCROLL:         Byte = 0x06
    const val KEY_PRESS:      Byte = 0x07
    const val KEY_COMBO:      Byte = 0x08
    const val PING:           Byte = 0x09
    const val MOUSE_MOVE_ABS: Byte = 0x0A
    const val STREAM_CONFIG:  Byte = 0x0B
    const val KEY_CHAR:       Byte = 0x0C
    const val CTRL_SCROLL:    Byte = 0x0D
    const val H_SCROLL:       Byte = 0x0E
    const val WIN_TAB:        Byte = 0x0F
    const val WIN_D:          Byte = 0x10
    const val ALT_LEFT:       Byte = 0x11
    const val ALT_RIGHT:      Byte = 0x12

    // Power
    const val SHUTDOWN:       Byte = 0x14
    const val RESTART:        Byte = 0x15
    const val SLEEP:          Byte = 0x16

    // Utilities (Aligned with C# Server)
    const val REQ_PROCESSES:  Byte = 0x17
    const val KILL_PROCESS:   Byte = 0x18
    const val TERMINAL_CMD:   Byte = 0x19
    const val REQ_FILES:      Byte = 0x1A
    const val FILE_DOWNLOAD:  Byte = 0x1B
    const val REQ_VOLUME:     Byte = 0x1C
    const val SET_VOLUME:     Byte = 0x1D
    const val SET_MUTE:       Byte = 0x1E

    // Response IDs (Server -> Android)
    const val RESP_PROCESSES: Byte = 0xA0.toByte()
    const val RESP_TERMINAL:  Byte = 0xA1.toByte()
    const val RESP_ERROR:     Byte = 0xA2.toByte()
    const val RESP_FILES:     Byte = 0xA3.toByte()
    const val RESP_PONG:      Byte = 0xA4.toByte()
    const val RESP_VOLUME:    Byte = 0xA5.toByte()

    fun mouseMove(dx: Int, dy: Int) = ByteBuffer.allocate(5).order(ByteOrder.BIG_ENDIAN).put(MOUSE_MOVE).putShort(dx.toShort()).putShort(dy.toShort()).array()
    fun mouseMoveAbs(x: Int, y: Int) = ByteBuffer.allocate(9).order(ByteOrder.BIG_ENDIAN).put(MOUSE_MOVE_ABS).putInt(x).putInt(y).array()
    fun mouseLeft() = byteArrayOf(MOUSE_LEFT)
    fun mouseRight() = byteArrayOf(MOUSE_RIGHT)
    fun mouseMiddle() = byteArrayOf(MOUSE_MIDDLE)
    fun mouseDouble() = byteArrayOf(MOUSE_DOUBLE)
    fun scroll(amt: Int) = ByteBuffer.allocate(3).order(ByteOrder.BIG_ENDIAN).put(SCROLL).putShort(amt.toShort()).array()
    fun hScroll(amt: Int) = ByteBuffer.allocate(5).order(ByteOrder.BIG_ENDIAN).put(H_SCROLL).putInt(amt).array()
    fun ctrlScroll(amt: Int) = ByteBuffer.allocate(5).order(ByteOrder.BIG_ENDIAN).put(CTRL_SCROLL).putInt(amt).array()
    fun keyPress(vk: Short) = ByteBuffer.allocate(3).order(ByteOrder.BIG_ENDIAN).put(KEY_PRESS).putShort(vk).array()
    fun keyCombo(mod: Short, key: Short) = ByteBuffer.allocate(5).order(ByteOrder.BIG_ENDIAN).put(KEY_COMBO).putShort(mod).putShort(key).array()
    fun keyChar(c: Short) = ByteBuffer.allocate(3).order(ByteOrder.BIG_ENDIAN).put(KEY_CHAR).putShort(c).array()
    fun ping() = byteArrayOf(PING)
    fun winTab() = byteArrayOf(WIN_TAB)
    fun winD() = byteArrayOf(WIN_D)
    fun altLeft() = byteArrayOf(ALT_LEFT)
    fun altRight() = byteArrayOf(ALT_RIGHT)
    fun shutdown() = byteArrayOf(SHUTDOWN)
    fun restart() = byteArrayOf(RESTART)
    fun sleep() = byteArrayOf(SLEEP)
    fun reqProcesses() = byteArrayOf(REQ_PROCESSES)
    fun killProcess(pid: Int) = ByteBuffer.allocate(5).order(ByteOrder.BIG_ENDIAN).put(KILL_PROCESS).putInt(pid).array()
    fun terminalCmd(cmd: String): ByteArray {
        val b = cmd.toByteArray()
        return ByteBuffer.allocate(1 + b.size).put(TERMINAL_CMD).put(b).array()
    }
    fun reqFiles(path: String): ByteArray {
        val b = path.toByteArray()
        return ByteBuffer.allocate(1 + b.size).put(REQ_FILES).put(b).array()
    }
    fun reqVolume() = byteArrayOf(REQ_VOLUME)
    fun setVolume(v: Int) = byteArrayOf(SET_VOLUME, v.toByte())
    fun setMute(m: Int) = byteArrayOf(SET_MUTE, m.toByte())

    fun streamConfig(config: StreamConfig) = ByteBuffer.allocate(11).order(ByteOrder.BIG_ENDIAN)
        .put(STREAM_CONFIG)
        .putInt(config.width)
        .putInt(config.height)
        .put(config.fps.toByte())
        .put(config.quality.toByte())
        .array()
}
