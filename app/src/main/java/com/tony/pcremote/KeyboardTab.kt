package com.tony.pcremote

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun KeyboardTab(viewModel: MainViewModel) {
    fun key(k: Short)              = viewModel.send(BinaryProtocol.keyPress(k))
    fun combo(mod: Short, k: Short) = viewModel.send(BinaryProtocol.keyCombo(mod, k))

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0D1117))
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {

        // Header
        Text(
            "Keyboard",
            color      = Color.White,
            fontWeight = FontWeight.Bold,
            fontSize   = 18.sp
        )

        // ── Function Keys ─────────────────────────────────────────
        KeySection("FUNCTION KEYS") {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                listOf(
                    "ESC" to KeyCodes.ESC,
                    "F4"  to KeyCodes.F4,
                    "F5"  to KeyCodes.F5,
                    "F11" to KeyCodes.F11,
                    "F12" to KeyCodes.F12
                ).forEach { (label, code) ->
                    KeyBtn(label, Modifier.weight(1f)) { key(code) }
                }
            }
        }

        // ── Navigation ────────────────────────────────────────────
        KeySection("NAVIGATION") {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                KeyBtn("TAB",   Modifier.weight(1f)) { key(KeyCodes.TAB) }
                KeyBtn("BKSP",  Modifier.weight(1f)) { key(KeyCodes.BACKSPACE) }
                KeyBtn("ENTER", Modifier.weight(1f)) { key(KeyCodes.ENTER) }
                KeyBtn("DEL",   Modifier.weight(1f)) { key(KeyCodes.DELETE) }
            }
        }

        // ── Modifiers ─────────────────────────────────────────────
        KeySection("MODIFIER KEYS") {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                ModKey("CTRL",  Color(0xFF1F3A5F), Modifier.weight(1f)) { key(KeyCodes.CTRL) }
                ModKey("ALT",   Color(0xFF1F3A5F), Modifier.weight(1f)) { key(KeyCodes.ALT) }
                ModKey("SHIFT", Color(0xFF1F3A5F), Modifier.weight(1f)) { key(KeyCodes.SHIFT) }
                ModKey("SPACE", Color(0xFF21262D), Modifier.weight(1.5f)) { key(KeyCodes.SPACE) }
            }
        }

        // ── Arrow Keys ────────────────────────────────────────────
        KeySection("ARROW KEYS") {
            Column(
                Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                KeyBtn("▲", Modifier.size(50.dp)) { key(KeyCodes.UP) }
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    KeyBtn("◀", Modifier.size(50.dp)) { key(KeyCodes.LEFT) }
                    KeyBtn("▼", Modifier.size(50.dp)) { key(KeyCodes.DOWN) }
                    KeyBtn("▶", Modifier.size(50.dp)) { key(KeyCodes.RIGHT) }
                }
            }
        }

        // ── Shortcuts ─────────────────────────────────────────────
        KeySection("SHORTCUTS") {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    ShortcutBtn("Copy  Ctrl+C",  Modifier.weight(1f)) { combo(KeyCodes.CTRL, KeyCodes.C) }
                    ShortcutBtn("Paste Ctrl+V",  Modifier.weight(1f)) { combo(KeyCodes.CTRL, KeyCodes.V) }
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    ShortcutBtn("Cut   Ctrl+X",  Modifier.weight(1f)) { combo(KeyCodes.CTRL, KeyCodes.X) }
                    ShortcutBtn("Undo  Ctrl+Z",  Modifier.weight(1f)) { combo(KeyCodes.CTRL, KeyCodes.Z) }
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    ShortcutBtn("Alt+F4",  Modifier.weight(1f)) { combo(KeyCodes.ALT,  KeyCodes.F4) }
                    ShortcutBtn("Ctrl+W",  Modifier.weight(1f)) { combo(KeyCodes.CTRL, KeyCodes.W) }
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    ShortcutBtn("Ctrl+T",  Modifier.weight(1f)) { combo(KeyCodes.CTRL, KeyCodes.T) }
                    ShortcutBtn("Win+E",   Modifier.weight(1f)) { combo(KeyCodes.WIN,  KeyCodes.E) }
                }
            }
        }
    }
}

@Composable
private fun KeySection(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier            = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(Color(0xFF161B22))
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text(
            title,
            color         = Color(0xFF8B949E),
            fontSize      = 10.sp,
            fontWeight    = FontWeight.Bold,
            letterSpacing = 1.sp
        )
        content()
    }
}

@Composable
fun KeyGroupLabel(text: String) {
    Text(text, color = Color(0xFF8B949E), fontSize = 11.sp, modifier = Modifier.padding(top = 2.dp))
}

@Composable
fun KeyBtn(label: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Button(
        onClick        = onClick,
        modifier       = modifier.height(48.dp),
        shape          = RoundedCornerShape(10.dp),
        colors         = ButtonDefaults.buttonColors(containerColor = Color(0xFF21262D)),
        contentPadding = PaddingValues(4.dp)
    ) {
        Text(label, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
    }
}

@Composable
private fun ModKey(label: String, color: Color, modifier: Modifier, onClick: () -> Unit) {
    Button(
        onClick        = onClick,
        modifier       = modifier.height(48.dp),
        shape          = RoundedCornerShape(10.dp),
        colors         = ButtonDefaults.buttonColors(containerColor = color),
        contentPadding = PaddingValues(4.dp)
    ) {
        Text(label, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF58A6FF))
    }
}

@Composable
fun ShortcutBtn(label: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Button(
        onClick        = onClick,
        modifier       = modifier.height(42.dp),
        shape          = RoundedCornerShape(10.dp),
        colors         = ButtonDefaults.buttonColors(containerColor = Color(0xFF21262D)),
        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Text(label, fontSize = 12.sp, color = Color(0xFF58A6FF), fontWeight = FontWeight.Medium)
    }
}
