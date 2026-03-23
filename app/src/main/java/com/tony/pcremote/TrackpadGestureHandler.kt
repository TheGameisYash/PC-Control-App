package com.tony.pcremote

import kotlinx.coroutines.*
import kotlin.math.abs

class TrackpadGestureHandler(
    private val send:        (ByteArray) -> Unit,
    private val scrollSpeed: Float = 3.5f
) {
    // Inertia
    private var inertiaJob: Job? = null
    var velX         = 0f
    var velY         = 0f
    var lastMoveTime = 0L

    // Double tap
    var lastTapTime  = 0L
    val doubleTapMs  = 350L

    // Scroll accumulator
    var scrollAccY = 0f
    var scrollAccX = 0f

    // ── Velocity tracking ─────────────────────────────────────────
    fun updateVelocity(dx: Float, dy: Float) {
        val now = System.currentTimeMillis()
        val dt  = (now - lastMoveTime).coerceIn(1L, 100L)
        velX         = (dx / dt * 16f).coerceIn(-30f, 30f)
        velY         = (dy / dt * 16f).coerceIn(-30f, 30f)
        lastMoveTime = now
    }

    // ── Inertia scroll (Mac-like coast after lift) ────────────────
    fun startInertia(scope: CoroutineScope) {
        inertiaJob?.cancel()
        if (abs(velX) < 0.3f && abs(velY) < 0.3f) return
        inertiaJob = scope.launch(Dispatchers.IO) {
            var vx = velX
            var vy = velY
            while (isActive && (abs(vx) > 0.3f || abs(vy) > 0.3f)) {
                val sy = (-vy * scrollSpeed).toInt().coerceIn(-15, 15)
                val sx = (vx  * scrollSpeed).toInt().coerceIn(-15, 15)
                if (sy != 0) send(BinaryProtocol.scroll(sy))
                if (sx != 0) send(BinaryProtocol.hScroll(sx))
                vx *= 0.88f
                vy *= 0.88f
                delay(16)
            }
        }
    }

    fun cancelInertia() = inertiaJob?.cancel()

    // ── Tap detection ─────────────────────────────────────────────
    fun handleTap(fingerCount: Int): String {
        return when (fingerCount) {
            1 -> {
                val now = System.currentTimeMillis()
                if (now - lastTapTime < doubleTapMs) {
                    send(BinaryProtocol.mouseDouble())
                    lastTapTime = 0L
                    "🖱️ Double Click"
                } else {
                    send(BinaryProtocol.mouseLeft())
                    lastTapTime = now
                    "🖱️ Left Click"
                }
            }
            2 -> { send(BinaryProtocol.mouseRight());  "🖱️ Right Click"  }
            3 -> { send(BinaryProtocol.mouseMiddle()); "🖱️ Middle Click" }
            else -> ""
        }
    }

    // ── Smooth scroll accumulator ─────────────────────────────────
    fun accumulateScroll(dCy: Float, dCx: Float): Pair<Int, Int> {
        scrollAccY += dCy * scrollSpeed
        scrollAccX += dCx * scrollSpeed
        var sy = 0; var sx = 0
        if (abs(scrollAccY) >= 8f) {
            sy = (scrollAccY / 8f).toInt().coerceIn(-20, 20)
            scrollAccY -= sy * 8f
        }
        if (abs(scrollAccX) >= 8f) {
            sx = (scrollAccX / 8f).toInt().coerceIn(-20, 20)
            scrollAccX -= sx * 8f
        }
        return sy to sx
    }

    fun resetScroll() {
        scrollAccX = 0f; scrollAccY = 0f
        velX = 0f;       velY = 0f
    }
}
