package com.tony.pcremote

import android.annotation.SuppressLint
import android.app.Activity
import android.content.pm.ActivityInfo
import android.content.res.Configuration
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.input.pointer.*
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.abs
import kotlin.math.hypot

@SuppressLint("SourceLockedOrientationActivity")
@Composable
fun RemoteDesktopScreen(
    viewModel: MainViewModel,
    userPrefs: UserPreferences,
    isPremium: Boolean,
    onUpgrade: () -> Unit,
    onBack:    () -> Unit
) {
    val context       = LocalContext.current
    val activity      = context as? Activity
    val configuration = LocalConfiguration.current
    val isLandscape   = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
    val screenFrame   by viewModel.screenFrame.collectAsState()
    val savedConfig   by userPrefs.streamConfig.collectAsState(initial = StreamConfig())
    val scope         = rememberCoroutineScope()

    var containerSize    by remember { mutableStateOf(IntSize.Zero) }
    var showResolution   by rememberSaveable { mutableStateOf(false) }
    var showKeyboard     by rememberSaveable { mutableStateOf(false) }
    var textInput        by rememberSaveable { mutableStateOf("") }
    var lastText         by rememberSaveable { mutableStateOf("") }

    var fpsCount by remember { mutableIntStateOf(0) }
    var fps      by remember { mutableIntStateOf(0) }

    val handler = remember {
        TrackpadGestureHandler(send = { viewModel.send(it) }, scrollSpeed = 3.5f)
    }

    LaunchedEffect(Unit) {
        while (true) { delay(1000); fps = fpsCount; fpsCount = 0 }
    }
    LaunchedEffect(screenFrame) { if (screenFrame != null) fpsCount++ }

    // Keyboard typing logic
    LaunchedEffect(textInput) {
        if (textInput.length > lastText.length) {
            textInput.substring(lastText.length).forEach { char ->
                viewModel.send(BinaryProtocol.keyChar(char.code.toShort()))
            }
        }
        lastText = textInput
    }

    LaunchedEffect(Unit) { viewModel.startScreenStream() }

    DisposableEffect(Unit) {
        onDispose {
            viewModel.stopScreenStream()
            handler.cancelInertia()
            activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        }
    }

    // ── OUTER wrapper ────────────────────────────────────────
    Box(modifier = Modifier.fillMaxSize()) {

        // ── INNER gesture box ─────────────────────────────────
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
                .onSizeChanged { containerSize = it }
                .pointerInput(containerSize) {
                    val sensitivity = 2.0f
                    val noiseGate   = 1.0f
                    val dragSlop    = 8f
                    val tapTimeout  = 200L
                    val longPressMs = 500L

                    awaitEachGesture {
                        handler.cancelInertia()
                        handler.resetScroll()

                        val firstDown    = awaitFirstDown(requireUnconsumed = false)
                        val gestureStart = System.currentTimeMillis()
                        val prevPos      = mutableMapOf(firstDown.id to firstDown.position)
                        var maxFingers   = 1
                        var dragging     = false
                        var consumed     = false
                        var pinchPrevDist = -1f

                        val lpJob = scope.launch {
                            delay(longPressMs)
                            if (!dragging && maxFingers == 1) {
                                viewModel.send(BinaryProtocol.mouseRight())
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
                                        val dur = System.currentTimeMillis() - gestureStart
                                        if (dur < tapTimeout) handler.handleTap(maxFingers)
                                    }
                                    if (maxFingers >= 2) handler.startInertia(scope)
                                    break
                                }

                                when (active.size) {
                                    1 -> {
                                        val c     = active[0]
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
                                                if (dx != 0 || dy != 0)
                                                    viewModel.send(BinaryProtocol.mouseMove(dx, dy))
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
                                        else hypot(
                                            (prevPos[c0.id]?.x ?: c0.position.x) - (prevPos[c1.id]?.x ?: c1.position.x),
                                            (prevPos[c0.id]?.y ?: c0.position.y) - (prevPos[c1.id]?.y ?: c1.position.y)
                                        )
                                        val dDist = curDist - prevDist
                                        val dCy   = prevMidY - curMidY
                                        val dCx   = prevMidX - curMidX

                                        if (abs(dDist) > 6f) {
                                            viewModel.send(BinaryProtocol.ctrlScroll(if (dDist > 0) 2 else -2))
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
                                            val dy3  = pCy3 - cy3
                                            val dx3  = pCx3 - cx3
                                            when {
                                                abs(dy3) > abs(dx3) && dy3 >  40f -> { viewModel.send(BinaryProtocol.winTab());   consumed = true }
                                                abs(dy3) > abs(dx3) && dy3 < -40f -> { viewModel.send(BinaryProtocol.winD());     consumed = true }
                                                abs(dx3) > abs(dy3) && dx3 >  40f -> { viewModel.send(BinaryProtocol.altRight()); consumed = true }
                                                abs(dx3) > abs(dy3) && dx3 < -40f -> { viewModel.send(BinaryProtocol.altLeft());  consumed = true }
                                            }
                                            pts.forEach { prevPos[it.id] = it.position; it.consume() }
                                        }
                                    }
                                }
                            }
                        } finally { lpJob.cancel() }
                    }
                }
        ) {
            // Screen frame
            if (screenFrame != null) {
                Image(
                    painter            = BitmapPainter(screenFrame!!),
                    contentDescription = "Remote Desktop",
                    modifier           = Modifier.fillMaxSize(),
                    contentScale       = ContentScale.Fit
                )
            } else {
                Column(
                    modifier            = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    CircularProgressIndicator(color = Color(0xFF2979FF), modifier = Modifier.size(40.dp))
                    Spacer(Modifier.height(12.dp))
                    Text("Connecting to PC...", color = Color(0xFF8B949E), fontSize = 14.sp)
                }
            }

            // Keyboard input
            AnimatedVisibility(
                visible  = showKeyboard,
                enter    = fadeIn() + slideInVertically(initialOffsetY = { it }),
                exit     = fadeOut() + slideOutVertically(targetOffsetY = { it }),
                modifier = Modifier.align(Alignment.BottomCenter)
            ) {
                OutlinedTextField(
                    value         = textInput,
                    onValueChange = { textInput = it },
                    label         = { Text("Type to send to PC", color = Color.Gray, fontSize = 11.sp) },
                    modifier      = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 10.dp)
                        .navigationBarsPadding(),
                    shape   = RoundedCornerShape(12.dp),
                    colors  = OutlinedTextFieldDefaults.colors(
                        focusedTextColor        = Color.White,
                        unfocusedTextColor      = Color.White,
                        focusedBorderColor      = Color(0xFF2979FF),
                        unfocusedBorderColor    = Color(0xFF30363D).copy(alpha = 0.6f),
                        focusedContainerColor   = Color(0xCC0D1117),
                        unfocusedContainerColor = Color(0xCC0D1117),
                        cursorColor             = Color(0xFF2979FF)
                    ),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
                    singleLine      = true
                )
            }

            // Resolution picker
            AnimatedVisibility(
                visible  = showResolution,
                enter    = fadeIn() + slideInVertically(),
                exit     = fadeOut() + slideOutVertically(),
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 56.dp)
                    .statusBarsPadding()
            ) {
                Card(
                    modifier = Modifier.padding(horizontal = 12.dp).fillMaxWidth(),
                    colors   = CardDefaults.cardColors(containerColor = Color(0xF2161B22)),
                    shape    = RoundedCornerShape(14.dp),
                    border   = BorderStroke(1.dp, Color(0xFF30363D))
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier              = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment     = Alignment.CenterVertically
                        ) {
                            Text("STREAM QUALITY", color = Color(0xFF8B949E),
                                fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(
                                            if (fps >= 25) Color(0xFF00C853).copy(alpha = 0.15f)
                                            else Color(0xFFFF9100).copy(alpha = 0.15f)
                                        )
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text("${fps}fps",
                                        color = if (fps >= 25) Color(0xFF00C853) else Color(0xFFFF9100),
                                        fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                }
                                Text("${savedConfig.width}×${savedConfig.height}",
                                    color = Color(0xFF8B949E), fontSize = 10.sp)
                            }
                        }
                        Spacer(Modifier.height(10.dp))
                        StreamPresets.all.forEach { (label, config) ->
                            val isLocked   = label.contains("⭐") && !isPremium
                            val isSelected = savedConfig == config
                            val cleanLabel = label.replace("⭐", "").trim()
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(
                                        if (isSelected) Color(0xFF2979FF).copy(alpha = 0.12f)
                                        else Color.Transparent
                                    )
                                    .clickable(enabled = !isLocked) {
                                        if (!isLocked) {
                                            scope.launch {
                                                userPrefs.saveStreamConfig(config)
                                                viewModel.send(BinaryProtocol.streamConfig(config))
                                            }
                                            showResolution = false
                                        } else onUpgrade()
                                    }
                                    .padding(horizontal = 12.dp, vertical = 9.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment     = Alignment.CenterVertically
                            ) {
                                Text(cleanLabel,
                                    color = if (isLocked) Color(0xFF484F58) else Color.White,
                                    fontSize = 13.sp,
                                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal)
                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    verticalAlignment = Alignment.CenterVertically) {
                                    Text("${config.width}×${config.height} · ${config.fps}fps",
                                        color = Color(0xFF8B949E), fontSize = 11.sp)
                                    if (isLocked)   Text("⭐", fontSize = 11.sp)
                                    if (isSelected) Text("✓",
                                        color = Color(0xFF2979FF), fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                }
                            }
                        }
                    }
                }
            }
        } // ── end inner gesture Box ────────────────────────────

        // ── NEW: Dedicated Back Button (Top Left) ─────────────
        IconButton(
            onClick  = onBack,
            modifier = Modifier
                .align(Alignment.TopStart)
                .statusBarsPadding()
                .padding(8.dp)
                .clip(CircleShape)
                .background(Color.Black.copy(alpha = 0.25f))
        ) {
            Icon(
                imageVector        = Icons.Default.ArrowBack,
                contentDescription = "Back",
                tint               = Color.White
            )
        }

        // ── Right-side utility column ─────────────────────────
        Column(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .statusBarsPadding()
                .padding(8.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
            horizontalAlignment = Alignment.End
        ) {
            GhostBtn(if (isLandscape) "📱" else "🔄") {
                if (isLandscape)
                    activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
                else
                    activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
            }
            GhostBtn("⌨️") { showKeyboard = !showKeyboard }
            GhostBtn("📺") { showResolution = !showResolution }
        }

    } // ── end outer Box ────────────────────────────────────────
}

@Composable
private fun GhostBtn(icon: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(40.dp) // Increased slightly for better tap targets
            .clip(RoundedCornerShape(10.dp))
            .background(Color.Black.copy(alpha = 0.25f))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(icon, fontSize = 18.sp)
    }
}