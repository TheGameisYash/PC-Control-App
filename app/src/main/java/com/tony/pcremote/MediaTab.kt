package com.tony.pcremote

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
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
fun MediaTab(viewModel: MainViewModel) {
    fun key(k: Short)              = viewModel.send(BinaryProtocol.keyPress(k))
    fun combo(mod: Short, k: Short) = viewModel.send(BinaryProtocol.keyCombo(mod, k))

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0D1117))
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {

        Text(
            "Media Controls",
            color      = Color.White,
            fontWeight = FontWeight.Bold,
            fontSize   = 18.sp
        )

        // ── Playback ──────────────────────────────────────────────
        MediaSection("PLAYBACK") {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment     = Alignment.CenterVertically
            ) {
                MediaBtn("⏮", Modifier.weight(1f)) { key(KeyCodes.MEDIA_PREV) }
                // Big play button
                Button(
                    onClick  = { key(KeyCodes.MEDIA_PLAY) },
                    modifier = Modifier.weight(1.6f).height(72.dp),
                    shape    = RoundedCornerShape(16.dp),
                    colors   = ButtonDefaults.buttonColors(containerColor = Color(0xFF2979FF))
                ) { Text("⏯", fontSize = 30.sp) }
                MediaBtn("⏭", Modifier.weight(1f)) { key(KeyCodes.MEDIA_NEXT) }
            }
            // Stop button
            Button(
                onClick  = { key(KeyCodes.MEDIA_STOP) },
                modifier = Modifier.fillMaxWidth().height(44.dp),
                shape    = RoundedCornerShape(12.dp),
                colors   = ButtonDefaults.buttonColors(containerColor = Color(0xFF21262D))
            ) {
                Text("⏹  Stop", color = Color(0xFF8B949E), fontSize = 14.sp)
            }
        }

        // ── Volume ────────────────────────────────────────────────
        MediaSection("VOLUME") {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick  = { key(KeyCodes.VOL_MUTE) },
                    modifier = Modifier.weight(0.8f).height(56.dp),
                    shape    = RoundedCornerShape(14.dp),
                    colors   = ButtonDefaults.buttonColors(containerColor = Color(0xFF21262D))
                ) { Text("🔇", fontSize = 22.sp) }
                Button(
                    onClick  = { key(KeyCodes.VOL_DOWN) },
                    modifier = Modifier.weight(1f).height(56.dp),
                    shape    = RoundedCornerShape(14.dp),
                    colors   = ButtonDefaults.buttonColors(containerColor = Color(0xFF21262D))
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("🔉", fontSize = 22.sp)
                        Text("Vol –", color = Color(0xFF8B949E), fontSize = 10.sp)
                    }
                }
                Button(
                    onClick  = { key(KeyCodes.VOL_UP) },
                    modifier = Modifier.weight(1f).height(56.dp),
                    shape    = RoundedCornerShape(14.dp),
                    colors   = ButtonDefaults.buttonColors(containerColor = Color(0xFF21262D))
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("🔊", fontSize = 22.sp)
                        Text("Vol +", color = Color(0xFF8B949E), fontSize = 10.sp)
                    }
                }
            }
        }

        // ── System ────────────────────────────────────────────────
        MediaSection("SYSTEM") {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                SysBtn("🔒", "Lock PC",     Modifier.weight(1f)) { combo(KeyCodes.WIN, KeyCodes.L) }
                SysBtn("🖥️", "Desktop",    Modifier.weight(1f)) { combo(KeyCodes.WIN, KeyCodes.D) }
                SysBtn("📷", "Screenshot", Modifier.weight(1f)) { key(KeyCodes.SNAPSHOT) }
            }
        }
    }
}

@Composable
private fun MediaSection(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(Color(0xFF161B22))
            .padding(14.dp),
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
fun MediaBtn(icon: String, modifier: Modifier, accent: Boolean = false, onClick: () -> Unit) {
    Button(
        onClick  = onClick,
        modifier = modifier.height(64.dp),
        shape    = RoundedCornerShape(14.dp),
        colors   = ButtonDefaults.buttonColors(
            containerColor = if (accent) Color(0xFF2979FF) else Color(0xFF21262D)
        )
    ) { Text(icon, fontSize = 26.sp) }
}

@Composable
fun SysBtn(icon: String, label: String, modifier: Modifier, onClick: () -> Unit) {
    Button(
        onClick  = onClick,
        modifier = modifier.height(64.dp),
        shape    = RoundedCornerShape(12.dp),
        colors   = ButtonDefaults.buttonColors(containerColor = Color(0xFF21262D))
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(icon, fontSize = 20.sp)
            Text(label, color = Color(0xFF8B949E), fontSize = 10.sp, fontWeight = FontWeight.Medium)
        }
    }
}
