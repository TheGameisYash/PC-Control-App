package com.tony.pcremote

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun KeyboardTab(viewModel: MainViewModel) {
    val haptic = LocalHapticFeedback.current
    val accentColor = Color(0xFF7C4DFF)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        // ── Quick Actions ─────────────────────────────────────
        Surface(
            modifier = Modifier.fillMaxWidth().height(80.dp),
            shape = RoundedCornerShape(24.dp),
            color = Color(0xFF121212),
            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.05f))
        ) {
            Row(
                modifier = Modifier.fillMaxSize().padding(horizontal = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                QuickActionItem("Esc", accentColor) { viewModel.send(BinaryProtocol.keyPress(KeyCodes.ESC)) }
                QuickActionItem("Win", accentColor) { viewModel.send(BinaryProtocol.keyPress(KeyCodes.WIN)) }
                QuickActionItem("Tab", accentColor) { viewModel.send(BinaryProtocol.keyPress(KeyCodes.TAB)) }
                QuickActionItem("Enter", accentColor) { viewModel.send(BinaryProtocol.keyPress(KeyCodes.ENTER)) }
            }
        }

        // ── Navigation & Control ──────────────────────────────
        ModernKeySection("NAVIGATION") {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                RepeatKeyBtn("Backspace", Icons.Rounded.Backspace, Modifier.weight(1f), KeyCodes.BACKSPACE, viewModel)
                RepeatKeyBtn("Space", Icons.Rounded.SpaceBar, Modifier.weight(1f), KeyCodes.SPACE, viewModel)
            }
            
            Spacer(Modifier.height(8.dp))
            
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    KeyButton("Del", Modifier.fillMaxWidth()) { viewModel.send(BinaryProtocol.keyPress(KeyCodes.DELETE)) }
                    KeyButton("Home", Modifier.fillMaxWidth()) { viewModel.send(BinaryProtocol.keyPress(KeyCodes.HOME)) }
                }
                
                // Arrow Cluster
                Surface(
                    modifier = Modifier.weight(1.2f).height(112.dp),
                    shape = RoundedCornerShape(20.dp),
                    color = Color(0xFF0A0A0A),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.03f))
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            ArrowKey(Icons.Rounded.KeyboardArrowUp) { viewModel.send(BinaryProtocol.keyPress(KeyCodes.UP)) }
                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                ArrowKey(Icons.Rounded.KeyboardArrowLeft) { viewModel.send(BinaryProtocol.keyPress(KeyCodes.LEFT)) }
                                ArrowKey(Icons.Rounded.KeyboardArrowDown) { viewModel.send(BinaryProtocol.keyPress(KeyCodes.DOWN)) }
                                ArrowKey(Icons.Rounded.KeyboardArrowRight) { viewModel.send(BinaryProtocol.keyPress(KeyCodes.RIGHT)) }
                            }
                        }
                    }
                }
            }
        }

        // ── Shortcuts ─────────────────────────────────────────
        ModernKeySection("SHORTCUTS") {
            val shortcuts = listOf(
                Triple("Select All", "Ctrl + A", KeyCodes.A),
                Triple("Copy", "Ctrl + C", KeyCodes.C),
                Triple("Paste", "Ctrl + V", KeyCodes.V),
                Triple("Undo", "Ctrl + Z", KeyCodes.Z)
            )
            
            shortcuts.chunked(2).forEach { row ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    row.forEach { (label, sub, code) ->
                        ShortcutCard(label, sub, Modifier.weight(1f)) { viewModel.send(BinaryProtocol.keyCombo(KeyCodes.CTRL, code)) }
                    }
                }
            }
        }

        // ── System ────────────────────────────────────────────
        ModernKeySection("SYSTEM") {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                KeyButton("Alt + F4", Modifier.weight(1f), Color(0xFFFF5252)) { viewModel.send(BinaryProtocol.keyCombo(KeyCodes.ALT, KeyCodes.F4)) }
                KeyButton("Ctrl+Alt+Del", Modifier.weight(1f)) { /* Special sequence if supported */ }
            }
        }

        Spacer(Modifier.height(40.dp))
    }
}

@Composable
fun QuickActionItem(label: String, color: Color, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        color = color.copy(alpha = 0.1f),
        border = BorderStroke(1.dp, color.copy(alpha = 0.2f))
    ) {
        Box(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp), contentAlignment = Alignment.Center) {
            Text(label, color = color, fontSize = 12.sp, fontWeight = FontWeight.Black)
        }
    }
}

@Composable
fun ModernKeySection(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            title, 
            color = Color.White.copy(alpha = 0.4f), 
            fontSize = 11.sp, 
            fontWeight = FontWeight.Bold, 
            letterSpacing = 1.sp
        )
        content()
    }
}

@Composable
fun KeyButton(label: String, modifier: Modifier = Modifier, color: Color = Color.White.copy(alpha = 0.05f), onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        modifier = modifier.height(50.dp),
        shape = RoundedCornerShape(16.dp),
        color = Color(0xFF161616),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.05f))
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(label, color = if (color == Color.White.copy(alpha = 0.05f)) Color.White else color, fontSize = 14.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun ArrowKey(icon: ImageVector, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        modifier = Modifier.size(34.dp),
        shape = RoundedCornerShape(8.dp),
        color = Color(0xFF1A1A1A),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.05f))
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(icon, null, tint = Color.White, modifier = Modifier.size(20.dp))
        }
    }
}

@Composable
fun ShortcutCard(label: String, sub: String, modifier: Modifier, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        modifier = modifier.height(64.dp),
        shape = RoundedCornerShape(16.dp),
        color = Color(0xFF121212),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.03f))
    ) {
        Column(modifier = Modifier.padding(horizontal = 16.dp), verticalArrangement = Arrangement.Center) {
            Text(label, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
            Text(sub, color = Color(0xFF7C4DFF), fontSize = 10.sp, fontWeight = FontWeight.Medium)
        }
    }
}

@Composable
fun RepeatKeyBtn(label: String, icon: ImageVector, modifier: Modifier, keyCode: Short, viewModel: MainViewModel) {
    val haptic = LocalHapticFeedback.current
    var isPressing by remember { mutableStateOf(false) }

    LaunchedEffect(isPressing) {
        if (isPressing) {
            viewModel.send(BinaryProtocol.keyPress(keyCode))
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            delay(400)
            while (isPressing) {
                viewModel.send(BinaryProtocol.keyPress(keyCode))
                delay(80)
            }
        }
    }

    Surface(
        modifier = modifier
            .height(56.dp)
            .pointerInput(Unit) {
                detectTapGestures(
                    onPress = {
                        isPressing = true
                        tryAwaitRelease()
                        isPressing = false
                    }
                )
            },
        shape = RoundedCornerShape(16.dp),
        color = if (isPressing) Color(0xFF222222) else Color(0xFF161616),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.05f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(icon, null, tint = Color.White.copy(alpha = 0.6f), modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text(label, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
        }
    }
}
