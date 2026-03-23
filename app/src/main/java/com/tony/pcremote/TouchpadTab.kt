package com.tony.pcremote

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.*
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.hypot

sealed class RemoteMode(val label: String, val icon: ImageVector) {
    object Mouse : RemoteMode("Mouse", Icons.Rounded.Mouse)
    object Media : RemoteMode("Media", Icons.Rounded.PlayArrow)
    object Power : RemoteMode("Power", Icons.Rounded.PowerSettingsNew)
    object Presentation : RemoteMode("PPT", Icons.Rounded.Slideshow)
    object Browser : RemoteMode("Web", Icons.Rounded.Language)
}

@Composable
fun TouchpadTab(viewModel: MainViewModel) {
    var gestureHint   by remember { mutableStateOf("") }
    var selectedMode  by remember { mutableStateOf<RemoteMode>(RemoteMode.Mouse) }
    
    val handler = remember { TrackpadGestureHandler(send = { viewModel.send(it) }, scrollSpeed = 3.5f) }

    LaunchedEffect(gestureHint) {
        if (gestureHint.isNotEmpty()) {
            delay(1000)
            gestureHint = ""
        }
    }

    val modes = remember { listOf(RemoteMode.Mouse, RemoteMode.Media, RemoteMode.Power, RemoteMode.Presentation, RemoteMode.Browser) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // ── Enhanced Mode Selector ──────────────────────────────
        Surface(
            modifier = Modifier.fillMaxWidth().height(64.dp),
            shape = RoundedCornerShape(20.dp),
            color = Color(0xFF121212),
            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.05f))
        ) {
            Row(
                modifier = Modifier.fillMaxSize().padding(horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                modes.forEach { mode ->
                    val isSelected = selectedMode == mode
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(if (isSelected) Color(0xFF00E5FF).copy(alpha = 0.1f) else Color.Transparent)
                            .clickable { selectedMode = mode },
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                mode.icon, 
                                null, 
                                tint = if (isSelected) Color(0xFF00E5FF) else Color.White.copy(alpha = 0.4f),
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                mode.label, 
                                fontSize = 10.sp, 
                                fontWeight = FontWeight.Bold,
                                color = if (isSelected) Color(0xFF00E5FF) else Color.White.copy(alpha = 0.4f)
                            )
                        }
                    }
                }
            }
        }

        // ── Dynamic Control Area ────────────────────────────────
        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
            Crossfade(
                targetState = selectedMode,
                animationSpec = tween(250, easing = FastOutSlowInEasing),
                label = "ModeTransition"
            ) { mode ->
                when (mode) {
                    is RemoteMode.Mouse -> {
                        TrackpadView(
                            viewModel = viewModel,
                            handler = handler,
                            onGesture = { gestureHint = it },
                            hint = gestureHint
                        )
                    }
                    is RemoteMode.Media -> MediaControls(viewModel)
                    is RemoteMode.Power -> PowerControls(viewModel)
                    is RemoteMode.Presentation -> PresentationControls(viewModel)
                    is RemoteMode.Browser -> BrowserControls(viewModel)
                }
            }
        }

        // ── Mouse Buttons ───────────────────────────────────────
        if (selectedMode == RemoteMode.Mouse) {
            Row(
                modifier = Modifier.fillMaxWidth().height(80.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                ModernButton("L", Modifier.weight(1.5f), Color(0xFF00E5FF)) { viewModel.send(BinaryProtocol.mouseLeft()) }
                ModernButton("M", Modifier.weight(1f), Color(0xFF7C4DFF)) { viewModel.send(BinaryProtocol.mouseMiddle()) }
                ModernButton("R", Modifier.weight(1.5f), Color(0xFF00E5FF)) { viewModel.send(BinaryProtocol.mouseRight()) }
            }
        }
    }
}

@Composable
fun TrackpadView(
    viewModel: MainViewModel,
    handler: TrackpadGestureHandler,
    onGesture: (String) -> Unit,
    hint: String
) {
    val scope = rememberCoroutineScope()

    Row(modifier = Modifier.fillMaxSize(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .clip(RoundedCornerShape(32.dp))
                .background(Color(0xFF0A0A0A))
                .border(1.dp, Color(0xFF00E5FF).copy(alpha = 0.2f), RoundedCornerShape(32.dp))
                .pointerInput(Unit) {
                    val sensitivity = 2.4f
                    val dragSlop    = 8f
                    val tapTimeout  = 200L
                    val longPressMs = 500L

                    awaitEachGesture {
                        handler.cancelInertia()
                        handler.resetScroll()
                        val firstDown = awaitFirstDown(requireUnconsumed = false)
                        val prevPos   = mutableMapOf(firstDown.id to firstDown.position)
                        var maxFingers = 1
                        var dragging   = false
                        var consumed   = false
                        var pinchPrevDist = -1f
                        val startTime  = System.currentTimeMillis()

                        val lpJob = scope.launch {
                            delay(longPressMs)
                            if (!dragging && maxFingers == 1) {
                                viewModel.send(BinaryProtocol.mouseRight())
                                onGesture("🖱️ Right Click")
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
                                        if (dur < tapTimeout) onGesture(handler.handleTap(maxFingers))
                                    }
                                    if (maxFingers >= 2) handler.startInertia(scope)
                                    break
                                }
                                when (active.size) {
                                    1 -> {
                                        val c = active[0]
                                        val prev = prevPos[c.id] ?: c.position
                                        val dx = ((c.position.x - prev.x) * sensitivity).toInt().coerceIn(-150, 150)
                                        val dy = ((c.position.y - prev.y) * sensitivity).toInt().coerceIn(-150, 150)
                                        if (abs(dx) > 0 || abs(dy) > 0) {
                                            if (!dragging && (abs(dx) > dragSlop || abs(dy) > dragSlop)) {
                                                dragging = true; lpJob.cancel()
                                            }
                                            if (dragging) viewModel.send(BinaryProtocol.mouseMove(dx, dy))
                                        }
                                        prevPos[c.id] = c.position; c.consume()
                                    }
                                    2 -> {
                                        lpJob.cancel(); dragging = true
                                        val c0 = active[0]; val c1 = active[1]
                                        val curDist = hypot(c0.position.x - c1.position.x, c0.position.y - c1.position.y)
                                        val curMidY = (c0.position.y + c1.position.y) / 2f
                                        val curMidX = (c0.position.x + c1.position.x) / 2f
                                        val prevMidY = ((prevPos[c0.id]?.y ?: curMidY) + (prevPos[c1.id]?.y ?: curMidY)) / 2f
                                        val prevMidX = ((prevPos[c0.id]?.x ?: curMidX) + (prevPos[c1.id]?.x ?: curMidX)) / 2f
                                        val prevDist = if (pinchPrevDist < 0f) curDist else pinchPrevDist
                                        
                                        if (abs(curDist - prevDist) > 15f) {
                                            viewModel.send(BinaryProtocol.ctrlScroll(if (curDist > prevDist) 2 else -2))
                                            onGesture(if (curDist > prevDist) "🔍 Zoom In" else "🔍 Zoom Out")
                                            pinchPrevDist = curDist
                                        } else {
                                            val dy = prevMidY - curMidY; val dx = prevMidX - curMidX
                                            handler.updateVelocity(dx, dy)
                                            val (sy, sx) = handler.accumulateScroll(dy, dx)
                                            if (sy != 0) viewModel.send(BinaryProtocol.scroll(sy))
                                            if (sx != 0) viewModel.send(BinaryProtocol.hScroll(sx))
                                        }
                                        active.forEach { prevPos[it.id] = it.position; it.consume() }
                                    }
                                    else -> {
                                        lpJob.cancel(); dragging = true
                                        if (!consumed) {
                                            val pts = active.take(3)
                                            val cy3 = pts.map { it.position.y }.average().toFloat()
                                            val pcy3 = pts.map { prevPos[it.id]?.y ?: it.position.y }.average().toFloat()
                                            val dy3 = pcy3 - cy3
                                            if (abs(dy3) > 40f) {
                                                if (dy3 > 0) { viewModel.send(BinaryProtocol.winTab()); onGesture("🗂 Task View") }
                                                else { viewModel.send(BinaryProtocol.winD()); onGesture("🖥 Desktop") }
                                                consumed = true
                                            }
                                        }
                                        active.forEach { prevPos[it.id] = it.position; it.consume() }
                                    }
                                }
                            }
                        } finally { lpJob.cancel() }
                    }
                },
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.fillMaxSize().graphicsLayer {
                renderEffect = null 
            }) {
                val step = 48.dp.toPx()
                for (x in 0..(size.width / step).toInt()) {
                    drawLine(Color(0xFF00E5FF).copy(alpha = 0.03f), Offset(x * step, 0f), Offset(x * step, size.height))
                }
                for (y in 0..(size.height / step).toInt()) {
                    drawLine(Color(0xFF00E5FF).copy(alpha = 0.03f), Offset(0f, y * step), Offset(size.width, y * step))
                }
            }
            
            if (hint.isNotEmpty()) {
                Surface(
                    color = Color(0xFF00E5FF).copy(alpha = 0.15f), 
                    shape = CircleShape,
                    modifier = Modifier.align(Alignment.Center)
                ) {
                    Text(
                        hint, 
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp), 
                        color = Color(0xFF00E5FF), 
                        fontWeight = FontWeight.Black, 
                        fontSize = 12.sp
                    )
                }
            }
        }

        Box(
            modifier = Modifier
                .width(48.dp)
                .fillMaxHeight()
                .clip(RoundedCornerShape(24.dp))
                .background(Color(0xFF0A0A0A))
                .border(1.dp, Color.White.copy(alpha = 0.05f), RoundedCornerShape(24.dp))
                .pointerInput(Unit) {
                    detectDragGestures { change, dragAmount ->
                        change.consume()
                        val sy = (dragAmount.y * -0.6f).toInt()
                        if (sy != 0) viewModel.send(BinaryProtocol.scroll(sy))
                    }
                },
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(40.dp)) {
                Icon(Icons.Rounded.KeyboardArrowUp, null, tint = Color.White.copy(alpha = 0.2f))
                Box(modifier = Modifier.size(4.dp, 100.dp).clip(CircleShape).background(Color(0xFF00E5FF).copy(alpha = 0.2f)))
                Icon(Icons.Rounded.KeyboardArrowDown, null, tint = Color.White.copy(alpha = 0.2f))
            }
        }
    }
}

@Composable
fun MediaControls(viewModel: MainViewModel) {
    ControlGrid(
        listOf(
            { ControlItem("Prev", Icons.Rounded.SkipPrevious, Color(0xFFFF4081)) { viewModel.send(BinaryProtocol.keyPress(KeyCodes.MEDIA_PREV)) } },
            { ControlItem("Play/Pause", Icons.Rounded.PlayCircle, Color(0xFF00E5FF)) { viewModel.send(BinaryProtocol.keyPress(KeyCodes.MEDIA_PLAY)) } },
            { ControlItem("Next", Icons.Rounded.SkipNext, Color(0xFFFF4081)) { viewModel.send(BinaryProtocol.keyPress(KeyCodes.MEDIA_NEXT)) } },
            { ControlItem("Vol -", Icons.AutoMirrored.Rounded.VolumeDown, Color(0xFF7C4DFF)) { viewModel.send(BinaryProtocol.keyPress(KeyCodes.VOL_DOWN)) } },
            { ControlItem("Mute", Icons.AutoMirrored.Rounded.VolumeOff, Color(0xFF444444)) { viewModel.send(BinaryProtocol.keyPress(KeyCodes.VOL_MUTE)) } },
            { ControlItem("Vol +", Icons.AutoMirrored.Rounded.VolumeUp, Color(0xFF7C4DFF)) { viewModel.send(BinaryProtocol.keyPress(KeyCodes.VOL_UP)) } }
        )
    )
}

@Composable
fun PowerControls(viewModel: MainViewModel) {
    var confirmType by remember { mutableStateOf<String?>(null) }
    
    if (confirmType != null) {
        AlertDialog(
            onDismissRequest = { confirmType = null },
            containerColor = Color(0xFF121212),
            title = { Text("Are you sure?", color = Color.White) },
            text = { Text("PC will ${confirmType?.lowercase()} immediately.", color = Color.White.copy(alpha = 0.6f)) },
            confirmButton = {
                Button(
                    onClick = { 
                        when (confirmType) {
                            "Shutdown" -> viewModel.shutdown()
                            "Restart"  -> viewModel.restart()
                            "Sleep"    -> viewModel.sleep()
                        }
                        confirmType = null 
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF5252))
                ) { Text("Confirm") }
            },
            dismissButton = {
                TextButton(onClick = { confirmType = null }) { Text("Cancel", color = Color.White.copy(alpha = 0.4f)) }
            }
        )
    }

    ControlGrid(
        listOf(
            { ControlItem("Lock", Icons.Rounded.Lock, Color(0xFF00E5FF)) { viewModel.send(BinaryProtocol.keyCombo(KeyCodes.WIN, KeyCodes.L)) } },
            { ControlItem("Sleep", Icons.Rounded.Bedtime, Color(0xFF7C4DFF)) { confirmType = "Sleep" } },
            { ControlItem("Restart", Icons.Rounded.RestartAlt, Color(0xFFFFAB40)) { confirmType = "Restart" } },
            { ControlItem("Shutdown", Icons.Rounded.PowerSettingsNew, Color(0xFFFF5252)) { confirmType = "Shutdown" } }
        )
    )
}

@Composable
fun PresentationControls(viewModel: MainViewModel) {
    ControlGrid(
        listOf(
            { ControlItem("Start (F5)", Icons.Rounded.PlayArrow, Color(0xFF00E5FF)) { viewModel.send(BinaryProtocol.keyPress(KeyCodes.F5)) } },
            { ControlItem("End (Esc)", Icons.Rounded.Close, Color(0xFFFF5252)) { viewModel.send(BinaryProtocol.keyPress(KeyCodes.ESC)) } },
            { ControlItem("Previous", Icons.AutoMirrored.Rounded.ArrowBack, Color(0xFF7C4DFF)) { viewModel.send(BinaryProtocol.keyPress(KeyCodes.LEFT)) } },
            { ControlItem("Next", Icons.AutoMirrored.Rounded.ArrowForward, Color(0xFF7C4DFF)) { viewModel.send(BinaryProtocol.keyPress(KeyCodes.RIGHT)) } }
        )
    )
}

@Composable
fun BrowserControls(viewModel: MainViewModel) {
    ControlGrid(
        listOf(
            { ControlItem("Back", Icons.AutoMirrored.Rounded.ArrowBackIos, Color(0xFF7C4DFF)) { viewModel.send(BinaryProtocol.altLeft()) } },
            { ControlItem("Refresh", Icons.Rounded.Refresh, Color(0xFF00E5FF)) { viewModel.send(BinaryProtocol.keyPress(KeyCodes.F5)) } },
            { ControlItem("Forward", Icons.AutoMirrored.Rounded.ArrowForwardIos, Color(0xFF7C4DFF)) { viewModel.send(BinaryProtocol.altRight()) } },
            { ControlItem("New Tab", Icons.Rounded.Add, Color(0xFFFF4081)) { viewModel.send(BinaryProtocol.keyCombo(KeyCodes.CTRL, KeyCodes.T)) } },
            { ControlItem("Close Tab", Icons.Rounded.Close, Color(0xFFFF5252)) { viewModel.send(BinaryProtocol.keyCombo(KeyCodes.CTRL, KeyCodes.W)) } },
            { ControlItem("Downloads", Icons.Rounded.Download, Color(0xFF00E5FF)) { viewModel.send(BinaryProtocol.keyCombo(KeyCodes.CTRL, KeyCodes.J)) } }
        )
    )
}

@Composable
fun ControlGrid(items: List<@Composable () -> Unit>) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(32.dp))
            .background(Color(0xFF0A0A0A))
            .border(1.dp, Color.White.copy(alpha = 0.05f), RoundedCornerShape(32.dp))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        items.chunked(2).forEach { rowItems ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                rowItems.forEach { item ->
                    Box(modifier = Modifier.weight(1f)) {
                        item()
                    }
                }
                if (rowItems.size == 1) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
fun ControlItem(label: String, icon: ImageVector, color: Color, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        modifier = Modifier.height(100.dp).fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        color = Color(0xFF161616),
        border = BorderStroke(1.dp, color.copy(alpha = 0.15f))
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(icon, null, tint = color, modifier = Modifier.size(32.dp))
            Spacer(Modifier.height(8.dp))
            Text(label, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun ModernButton(label: String, modifier: Modifier, color: Color, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        modifier = modifier.fillMaxHeight(),
        shape = RoundedCornerShape(24.dp),
        color = Color(0xFF121212),
        border = BorderStroke(1.dp, color.copy(alpha = 0.2f))
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                label, 
                color = Color.White, 
                fontWeight = FontWeight.Black, 
                fontSize = 18.sp
            )
        }
    }
}
