package com.tony.pcremote

import android.app.Activity
import android.content.pm.ActivityInfo
import android.content.res.Configuration
import androidx.compose.animation.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlin.math.roundToInt

@Composable
fun ScreenTab(viewModel: MainViewModel, onDisconnect: () -> Unit) {
    val screenFrame    by viewModel.screenFrame.collectAsState()
    val context        = LocalContext.current
    val activity       = context as? Activity
    val configuration  = LocalConfiguration.current
    val isLandscape    = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

    var showOverlay    by remember { mutableStateOf(true) }
    var containerSize  by remember { mutableStateOf(IntSize.Zero) }
    var accumX         by remember { mutableStateOf(0f) }
    var accumY         by remember { mutableStateOf(0f) }

    // Auto-hide overlay after 4 seconds of inactivity
    LaunchedEffect(showOverlay) {
        if (showOverlay) {
            delay(4000)
            showOverlay = false
        }
    }

    // Start stream only when this tab is visible
    LaunchedEffect(Unit) { viewModel.startScreenStream() }
    DisposableEffect(Unit) {
        onDispose {
            viewModel.stopScreenStream()
            // Restore portrait when leaving screen tab
            activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .onSizeChanged { containerSize = it }
            // Drag = relative cursor move (smooth control)
            .pointerInput(containerSize) {
                detectDragGestures(
                    onDragStart = { accumX = 0f; accumY = 0f },
                    onDrag = { change, dragAmount ->
                        change.consume()
                        accumX += dragAmount.x * 1.5f
                        accumY += dragAmount.y * 1.5f
                        val dx = accumX.toInt()
                        val dy = accumY.toInt()
                        accumX -= dx; accumY -= dy
                        if (dx != 0 || dy != 0)
                            viewModel.send(BinaryProtocol.mouseMove(dx, dy))
                    }
                )
            }
            // Tap = move cursor to exact position + click (like Monect)
            .pointerInput(containerSize) {
                detectTapGestures(
                    onTap = { offset ->
                        // Move cursor to exact tapped position on PC
                        viewModel.sendAbsMouseMove(
                            offset.x, offset.y,
                            containerSize.width, containerSize.height
                        )
                        viewModel.send(BinaryProtocol.mouseLeft())
                        showOverlay = !showOverlay
                    },
                    onDoubleTap = { offset ->
                        viewModel.sendAbsMouseMove(
                            offset.x, offset.y,
                            containerSize.width, containerSize.height
                        )
                        viewModel.send(BinaryProtocol.mouseDouble())
                    },
                    onLongPress = { offset ->
                        viewModel.sendAbsMouseMove(
                            offset.x, offset.y,
                            containerSize.width, containerSize.height
                        )
                        viewModel.send(BinaryProtocol.mouseRight())
                    }
                )
            }
    ) {
        // ── Screen frame or loading indicator ─────────────────
        if (screenFrame != null) {
            Image(
                bitmap = screenFrame!!,
                contentDescription = "PC Screen",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Fit
            )
        } else {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                CircularProgressIndicator(color = Color(0xFF2979FF))
                Spacer(Modifier.height(16.dp))
                Text("Connecting to screen stream...", color = Color(0xFF8B949E))
                Spacer(Modifier.height(8.dp))
                Text("Make sure PC server is running", color = Color(0xFF484F58),
                    fontSize = 12.sp)
            }
        }

        // ── Top overlay bar ────────────────────────────────────
        AnimatedVisibility(
            visible = showOverlay,
            enter = fadeIn() + slideInVertically(),
            exit  = fadeOut() + slideOutVertically(),
            modifier = Modifier.align(Alignment.TopCenter)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xCC000000))
                    .statusBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("🖥️ PC Screen", color = Color.White,
                    fontWeight = FontWeight.Bold, fontSize = 16.sp)

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    // Scroll buttons compact
                    SmallControlButton("▲") {
                        viewModel.send(BinaryProtocol.scroll(3))
                    }
                    SmallControlButton("▼") {
                        viewModel.send(BinaryProtocol.scroll(-3))
                    }
                    // Rotate button
                    SmallControlButton(if (isLandscape) "📱" else "🔄") {
                        activity?.requestedOrientation =
                            if (isLandscape) ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
                            else ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
                    }
                }
            }
        }

        // ── Bottom overlay — mouse buttons ─────────────────────
        AnimatedVisibility(
            visible = showOverlay,
            enter = fadeIn() + slideInVertically(initialOffsetY = { it }),
            exit  = fadeOut() + slideOutVertically(targetOffsetY = { it }),
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xCC000000))
                    .navigationBarsPadding()
                    .padding(horizontal = 12.dp, vertical = 8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = { viewModel.send(BinaryProtocol.mouseLeft()) },
                        modifier = Modifier.weight(1f).height(44.dp),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2979FF))
                    ) { Text("LEFT", fontWeight = FontWeight.Bold, fontSize = 13.sp) }

                    Button(
                        onClick = { viewModel.send(BinaryProtocol.mouseMiddle()) },
                        modifier = Modifier.weight(0.7f).height(44.dp),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF37474F))
                    ) { Text("MID", fontSize = 12.sp) }

                    Button(
                        onClick = { viewModel.send(BinaryProtocol.mouseRight()) },
                        modifier = Modifier.weight(1f).height(44.dp),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF455A64))
                    ) { Text("RIGHT", fontWeight = FontWeight.Bold, fontSize = 13.sp) }
                }

                Spacer(Modifier.height(4.dp))
                Text(
                    "Tap = click  •  Drag = move  •  Long press = right click  •  Tap to toggle overlay",
                    color = Color(0xFF484F58), fontSize = 10.sp,
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                )
            }
        }
    }
}

@Composable
fun SmallControlButton(label: String, onClick: () -> Unit) {
    FilledTonalButton(
        onClick = onClick,
        modifier = Modifier.size(36.dp),
        shape = RoundedCornerShape(8.dp),
        contentPadding = PaddingValues(0.dp),
        colors = ButtonDefaults.filledTonalButtonColors(
            containerColor = Color(0xFF2D333B)
        )
    ) { Text(label, fontSize = 14.sp) }
}
