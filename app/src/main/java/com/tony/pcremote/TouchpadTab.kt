package com.tony.pcremote

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.*
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.hypot

@Composable
fun TouchpadTab(viewModel: MainViewModel) {
    var containerSize by remember { mutableStateOf(IntSize.Zero) }
    var gestureHint   by remember { mutableStateOf("") }
    val scope         = rememberCoroutineScope()

    val handler = remember {
        TrackpadGestureHandler(send = { viewModel.send(it) }, scrollSpeed = 3.5f)
    }

    LaunchedEffect(gestureHint) {
        if (gestureHint.isNotEmpty()) { delay(1200); gestureHint = "" }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0D1117))
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {

        // ── Header ────────────────────────────────────────────────
        Row(
            modifier              = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment     = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    "Trackpad",
                    color      = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize   = 18.sp
                )
                Text(
                    "Touch surface below to control cursor",
                    color    = Color(0xFF8B949E),
                    fontSize = 11.sp
                )
            }
            AnimatedVisibility(visible = gestureHint.isNotEmpty(), enter = fadeIn(), exit = fadeOut()) {
                Text(
                    gestureHint,
                    color      = Color(0xFF2979FF),
                    fontSize   = 12.sp,
                    fontWeight = FontWeight.Medium,
                    modifier   = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(Color(0xFF2979FF).copy(alpha = 0.12f))
                        .border(1.dp, Color(0xFF2979FF).copy(alpha = 0.3f), RoundedCornerShape(20.dp))
                        .padding(horizontal = 12.dp, vertical = 5.dp)
                )
            }
        }

        // ── Trackpad Surface ──────────────────────────────────────
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .clip(RoundedCornerShape(18.dp))
                .background(Color(0xFF161B22))
                .border(1.dp, Color(0xFF30363D), RoundedCornerShape(18.dp))
                .onSizeChanged { containerSize = it }
                .pointerInput(containerSize) {
                    val sensitivity = 2.2f
                    val noiseGate   = 1.0f
                    val dragSlop    = 8f
                    val tapTimeout  = 220L
                    val longPressMs = 550L

                    awaitEachGesture {
                        handler.cancelInertia()
                        handler.resetScroll()
                        val firstDown = awaitFirstDown(requireUnconsumed = false)
                        val prevPos   = mutableMapOf(firstDown.id to firstDown.position)
                        var maxFingers    = 1
                        var dragging      = false
                        var consumed      = false
                        var pinchPrevDist = -1f
                        val startTime     = System.currentTimeMillis()

                        val lpJob = scope.launch {
                            delay(longPressMs)
                            if (!dragging && maxFingers == 1) {
                                viewModel.send(BinaryProtocol.mouseRight())
                                gestureHint = "🖱️ Right Click"
                                consumed = true
                            }
                        }
                        try {
                            while (true) {
                                val event  = awaitPointerEvent(PointerEventPass.Main)
                                val active = event.changes.filter { it.pressed }
                                if (active.size > maxFingers) maxFingers = active.size
                                if (active.isEmpty()) {
                                    lpJob.cancel()
                                    if (!dragging && !consumed) {
                                        val dur = System.currentTimeMillis() - startTime
                                        if (dur < tapTimeout) gestureHint = handler.handleTap(maxFingers)
                                    }
                                    if (maxFingers >= 2) handler.startInertia(scope)
                                    break
                                }
                                when (active.size) {
                                    1 -> {
                                        val c = active[0]
                                        val prev  = prevPos[c.id] ?: c.position
                                        val rawDx = c.position.x - prev.x
                                        val rawDy = c.position.y - prev.y
                                        if (abs(rawDx) >= noiseGate || abs(rawDy) >= noiseGate) {
                                            if (!dragging && (abs(rawDx) > dragSlop || abs(rawDy) > dragSlop)) {
                                                dragging = true; lpJob.cancel()
                                            }
                                            if (dragging) {
                                                val dx = (rawDx * sensitivity).toInt().coerceIn(-150, 150)
                                                val dy = (rawDy * sensitivity).toInt().coerceIn(-150, 150)
                                                if (dx != 0 || dy != 0) viewModel.send(BinaryProtocol.mouseMove(dx, dy))
                                            }
                                        }
                                        prevPos[c.id] = c.position; c.consume()
                                    }
                                    2 -> {
                                        lpJob.cancel(); dragging = true
                                        val c0 = active[0]; val c1 = active[1]
                                        val curDist  = hypot(c0.position.x - c1.position.x, c0.position.y - c1.position.y)
                                        val curMidX  = (c0.position.x + c1.position.x) / 2f
                                        val curMidY  = (c0.position.y + c1.position.y) / 2f
                                        val prevMidX = ((prevPos[c0.id]?.x ?: curMidX) + (prevPos[c1.id]?.x ?: curMidX)) / 2f
                                        val prevMidY = ((prevPos[c0.id]?.y ?: curMidY) + (prevPos[c1.id]?.y ?: curMidY)) / 2f
                                        val prevDist = if (pinchPrevDist < 0f) { pinchPrevDist = curDist; curDist }
                                        else hypot((prevPos[c0.id]?.x ?: c0.position.x) - (prevPos[c1.id]?.x ?: c1.position.x),
                                            (prevPos[c0.id]?.y ?: c0.position.y) - (prevPos[c1.id]?.y ?: c1.position.y))
                                        val dDist = curDist - prevDist
                                        val dCy = prevMidY - curMidY; val dCx = prevMidX - curMidX
                                        if (abs(dDist) > 6f) {
                                            viewModel.send(BinaryProtocol.ctrlScroll(if (dDist > 0) 2 else -2))
                                            gestureHint = if (dDist > 0) "🔍 Zoom In" else "🔍 Zoom Out"
                                            pinchPrevDist = curDist
                                        } else {
                                            handler.updateVelocity(dCx, dCy)
                                            val (sy, sx) = handler.accumulateScroll(dCy, dCx)
                                            if (sy != 0) viewModel.send(BinaryProtocol.scroll(sy))
                                            if (sx != 0) viewModel.send(BinaryProtocol.hScroll(sx))
                                        }
                                        pinchPrevDist = curDist
                                        active.forEach { prevPos[it.id] = it.position; it.consume() }
                                    }
                                    else -> {
                                        lpJob.cancel(); dragging = true
                                        if (!consumed) {
                                            val pts  = active.take(3)
                                            val cy3  = pts.sumOf { it.position.y.toDouble() }.toFloat() / 3f
                                            val cx3  = pts.sumOf { it.position.x.toDouble() }.toFloat() / 3f
                                            val pCy3 = pts.sumOf { (prevPos[it.id]?.y ?: cy3).toDouble() }.toFloat() / 3f
                                            val pCx3 = pts.sumOf { (prevPos[it.id]?.x ?: cx3).toDouble() }.toFloat() / 3f
                                            val dy3 = pCy3 - cy3; val dx3 = pCx3 - cx3
                                            when {
                                                abs(dy3) > abs(dx3) && dy3 > 40f  -> { viewModel.send(BinaryProtocol.winTab());   gestureHint = "🗂 Task View";    consumed = true }
                                                abs(dy3) > abs(dx3) && dy3 < -40f -> { viewModel.send(BinaryProtocol.winD());     gestureHint = "🖥 Show Desktop"; consumed = true }
                                                abs(dx3) > abs(dy3) && dx3 > 40f  -> { viewModel.send(BinaryProtocol.altRight()); gestureHint = "→ Forward";       consumed = true }
                                                abs(dx3) > abs(dy3) && dx3 < -40f -> { viewModel.send(BinaryProtocol.altLeft());  gestureHint = "← Back";          consumed = true }
                                            }
                                            pts.forEach { prevPos[it.id] = it.position; it.consume() }
                                        }
                                    }
                                }
                            }
                        } finally { lpJob.cancel() }
                    }
                },
            contentAlignment = Alignment.Center
        ) {
            if (gestureHint.isEmpty()) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier            = Modifier.padding(24.dp)
                ) {
                    Text("🖱️", fontSize = 32.sp)
                    Text(
                        "Move your finger to control cursor",
                        color     = Color(0xFF484F58),
                        fontSize  = 13.sp,
                        textAlign = TextAlign.Center
                    )
                    Spacer(Modifier.height(4.dp))
                    Column(
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        listOf(
                            "1 finger  →  move cursor",
                            "2 fingers →  scroll",
                            "Pinch     →  zoom",
                            "2-tap     →  right click",
                            "3-swipe ↑ →  Task View",
                            "3-swipe ↓ →  Show Desktop"
                        ).forEach {
                            Text(it, color = Color(0xFF30363D), fontSize = 11.sp)
                        }
                    }
                }
            }
        }

        // ── Mouse Buttons ─────────────────────────────────────────
        Row(
            modifier              = Modifier.fillMaxWidth().height(56.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = { viewModel.send(BinaryProtocol.mouseLeft()); gestureHint = "◀ Left Click" },
                modifier = Modifier.weight(1f).fillMaxHeight(),
                shape    = RoundedCornerShape(14.dp),
                colors   = ButtonDefaults.buttonColors(containerColor = Color(0xFF2979FF))
            ) {
                Text("LEFT", fontWeight = FontWeight.Bold, fontSize = 13.sp)
            }
            Button(
                onClick = { viewModel.send(BinaryProtocol.mouseMiddle()); gestureHint = "● Middle" },
                modifier = Modifier.width(52.dp).fillMaxHeight(),
                shape    = RoundedCornerShape(14.dp),
                colors   = ButtonDefaults.buttonColors(containerColor = Color(0xFF21262D))
            ) { Text("●", fontSize = 16.sp, color = Color(0xFF8B949E)) }
            Button(
                onClick = { viewModel.send(BinaryProtocol.mouseRight()); gestureHint = "Right Click ▶" },
                modifier = Modifier.weight(1f).fillMaxHeight(),
                shape    = RoundedCornerShape(14.dp),
                colors   = ButtonDefaults.buttonColors(containerColor = Color(0xFF21262D))
            ) {
                Text("RIGHT", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color(0xFF8B949E))
            }
        }

        // ── Scroll / Nav Row ──────────────────────────────────────
        Row(
            modifier              = Modifier.fillMaxWidth().height(44.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            listOf(
                "◀◀" to { viewModel.send(BinaryProtocol.altLeft()) },
                "▲"  to { viewModel.send(BinaryProtocol.scroll(5)) },
                "▼"  to { viewModel.send(BinaryProtocol.scroll(-5)) },
                "▶▶" to { viewModel.send(BinaryProtocol.altRight()) }
            ).forEach { (label, action) ->
                Button(
                    onClick  = action,
                    modifier = Modifier.weight(1f).fillMaxHeight(),
                    shape    = RoundedCornerShape(10.dp),
                    colors   = ButtonDefaults.buttonColors(containerColor = Color(0xFF161B22))
                ) {
                    Text(label, color = Color(0xFF8B949E), fontSize = 14.sp)
                }
            }
        }
    }
}
