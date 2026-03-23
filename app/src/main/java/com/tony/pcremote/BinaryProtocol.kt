package com.tony.pcremote

import java.nio.ByteBuffer
import java.nio.ByteOrder

// All packets use BIG_ENDIAN to match Java's DataInputStream on Android
// Packet format: [1 byte type] [payload bytes]
object BinaryProtocol {

    // Command type IDs — must match C# server exactly
    const val MOUSE_MOVE:     Byte = 0x01
    const val MOUSE_LEFT:     Byte = 0x02
    const val MOUSE_RIGHT:    Byte = 0x03
    const val MOUSE_MIDDLE:   Byte = 0x04
    const val MOUSE_DOUBLE:   Byte = 0x05
    const val SCROLL:         Byte = 0x06
    const val KEY_PRESS:      Byte = 0x07
    const val KEY_COMBO:      Byte = 0x08
    const val PING:           Byte = 0x09

    // Existing features
    const val MOUSE_MOVE_ABS: Byte = 0x0A
    const val STREAM_CONFIG:  Byte = 0x0B
    const val KEY_CHAR:       Byte = 0x0C

    // ✅ NEW (continued safely)
    const val CTRL_SCROLL:    Byte = 0x0D
    const val H_SCROLL:       Byte = 0x0E
    const val WIN_TAB:        Byte = 0x0F
    const val WIN_D:          Byte = 0x10
    const val ALT_LEFT:       Byte = 0x11
    const val ALT_RIGHT:      Byte = 0x12
    const val MOUSE_MIDDLE_NEW: Byte = 0x13 // optional separate if needed

    // Mouse move: [0x01][dx Int16][dy Int16]
    fun mouseMove(dx: Int, dy: Int): ByteArray =
        ByteBuffer.allocate(5).order(ByteOrder.BIG_ENDIAN)
            .put(MOUSE_MOVE)
            .putShort(dx.toShort())
            .putShort(dy.toShort())
            .array()

    // Mouse click
    fun mouseLeft()   = byteArrayOf(MOUSE_LEFT)
    fun mouseRight()  = byteArrayOf(MOUSE_RIGHT)
    fun mouseMiddle() = byteArrayOf(MOUSE_MIDDLE)
    fun mouseDouble() = byteArrayOf(MOUSE_DOUBLE)

    // Scroll
    fun scroll(amount: Int): ByteArray =
        ByteBuffer.allocate(3).order(ByteOrder.BIG_ENDIAN)
            .put(SCROLL)
            .putShort(amount.toShort())
            .array()

    // Key press
    fun keyPress(vkCode: Short): ByteArray =
        ByteBuffer.allocate(3).order(ByteOrder.BIG_ENDIAN)
            .put(KEY_PRESS)
            .putShort(vkCode)
            .array()

    // Key combo
    fun keyCombo(modCode: Short, keyCode: Short): ByteArray =
        ByteBuffer.allocate(5).order(ByteOrder.BIG_ENDIAN)
            .put(KEY_COMBO)
            .putShort(modCode)
            .putShort(keyCode)
            .array()

    // Ping
    fun ping() = byteArrayOf(PING)

    // Absolute mouse
    fun mouseMoveAbs(pcX: Int, pcY: Int): ByteArray =
        ByteBuffer.allocate(9).order(ByteOrder.BIG_ENDIAN)
            .put(MOUSE_MOVE_ABS)
            .putInt(pcX)
            .putInt(pcY)
            .array()

    // Stream configuration
    fun streamConfig(config: StreamConfig): ByteArray =
        ByteBuffer.allocate(7).order(ByteOrder.BIG_ENDIAN)
            .put(STREAM_CONFIG)
            .putShort(config.width.toShort())
            .putShort(config.height.toShort())
            .put(config.fps.toByte())
            .put(config.quality.toByte())
            .array()

    // Character typing
    fun keyChar(charCode: Short): ByteArray =
        ByteBuffer.allocate(3).order(ByteOrder.BIG_ENDIAN)
            .put(KEY_CHAR)
            .putShort(charCode)
            .array()

    // =========================
    // ✅ NEW FUNCTIONS ADDED
    // =========================

    // Ctrl + Scroll (pinch zoom)
    fun ctrlScroll(dir: Int): ByteArray =
        ByteBuffer.allocate(5).order(ByteOrder.BIG_ENDIAN)
            .put(CTRL_SCROLL)
            .putInt(dir)
            .array()

    // Horizontal scroll
    fun hScroll(amount: Int): ByteArray =
        ByteBuffer.allocate(5).order(ByteOrder.BIG_ENDIAN)
            .put(H_SCROLL)
            .putInt(amount)
            .array()

    // Win + Tab
    fun winTab(): ByteArray = byteArrayOf(WIN_TAB)

    // Win + D
    fun winD(): ByteArray = byteArrayOf(WIN_D)

    // Alt + Left
    fun altLeft(): ByteArray = byteArrayOf(ALT_LEFT)

    // Alt + Right
    fun altRight(): ByteArray = byteArrayOf(ALT_RIGHT)
}