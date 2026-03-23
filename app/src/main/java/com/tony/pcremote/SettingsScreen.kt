package com.tony.pcremote

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch

@Composable
fun SettingsScreen(
    userPrefs:   UserPreferences,
    isPremium:   Boolean,
    viewModel:   MainViewModel,
    onBack:      () -> Unit,
    onUpgrade:   () -> Unit,
    onLoggedOut: () -> Unit
) {
    val scope          = rememberCoroutineScope()
    val savedConfig    by userPrefs.streamConfig.collectAsState(initial = StreamConfig())
    val username       by userPrefs.username.collectAsState(initial = "")
    val isPremiumState by userPrefs.isPremium.collectAsState(initial = false)

    var selected by remember { mutableStateOf(savedConfig) }
    var showLogoutDialog by remember { mutableStateOf(false) }

    LaunchedEffect(savedConfig) { selected = savedConfig }

    // ── Logout Confirmation Dialog ────────────────────────────────
    if (showLogoutDialog) {
        AlertDialog(
            onDismissRequest = { showLogoutDialog = false },
            containerColor   = Color(0xFF161B22),
            shape            = RoundedCornerShape(16.dp),
            title = {
                Text("Logout?", color = Color.White, fontWeight = FontWeight.Bold)
            },
            text = {
                Text(
                    "You'll need to login again to use PC Remote.",
                    color = Color(0xFF8B949E), fontSize = 14.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = { scope.launch { userPrefs.logout(); onLoggedOut() } },
                    colors  = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF5252)),
                    shape   = RoundedCornerShape(10.dp)
                ) { Text("Logout", fontWeight = FontWeight.Bold) }
            },
            dismissButton = {
                TextButton(onClick = { showLogoutDialog = false }) {
                    Text("Cancel", color = Color(0xFF8B949E))
                }
            }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0D1117))
    ) {

        // ── Top Bar ───────────────────────────────────────────────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF161B22))
                .statusBarsPadding()
                .padding(horizontal = 8.dp, vertical = 10.dp),
            verticalAlignment     = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            TextButton(onClick = onBack) {
                Text("← Back", color = Color(0xFF2979FF), fontSize = 14.sp)
            }
            Text(
                "Settings",
                color      = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize   = 16.sp
            )
            Spacer(Modifier.width(72.dp))
        }

        // ── Scrollable Content ────────────────────────────────────
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {

            // ── Account Card ──────────────────────────────────────
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape    = RoundedCornerShape(16.dp),
                colors   = CardDefaults.cardColors(containerColor = Color(0xFF161B22)),
                border   = BorderStroke(
                    1.dp,
                    if (isPremiumState) Color(0xFFFFD700).copy(alpha = 0.3f)
                    else Color(0xFF30363D)
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment     = Alignment.CenterVertically
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment     = Alignment.CenterVertically
                    ) {
                        // Avatar
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF2979FF).copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                username.firstOrNull()?.uppercaseChar()?.toString() ?: "?",
                                color      = Color(0xFF2979FF),
                                fontSize   = 18.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Column {
                            Text(
                                username.ifEmpty { "Not logged in" },
                                color      = Color.White,
                                fontSize   = 14.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Spacer(Modifier.height(2.dp))
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                verticalAlignment     = Alignment.CenterVertically
                            ) {
                                if (isPremiumState) {
                                    Icon(
                                        Icons.Default.Star,
                                        contentDescription = null,
                                        tint     = Color(0xFFFFD700),
                                        modifier = Modifier.size(12.dp)
                                    )
                                    Text(
                                        "Premium",
                                        color      = Color(0xFFFFD700),
                                        fontSize   = 12.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                } else {
                                    Text("Free Plan", color = Color(0xFF8B949E), fontSize = 12.sp)
                                }
                            }
                        }
                    }
                    Column(
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                        horizontalAlignment = Alignment.End
                    ) {
                        if (!isPremiumState) {
                            TextButton(
                                onClick        = onUpgrade,
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                shape          = RoundedCornerShape(8.dp),
                                colors         = ButtonDefaults.textButtonColors(
                                    containerColor = Color(0xFFFFD700).copy(alpha = 0.1f)
                                )
                            ) {
                                Text(
                                    "Upgrade ⭐",
                                    color      = Color(0xFFFFD700),
                                    fontSize   = 12.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                        TextButton(
                            onClick        = { showLogoutDialog = true },
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                            shape          = RoundedCornerShape(8.dp),
                            colors         = ButtonDefaults.textButtonColors(
                                containerColor = Color(0xFFFF5252).copy(alpha = 0.08f)
                            )
                        ) {
                            Text(
                                "Logout",
                                color      = Color(0xFFFF5252),
                                fontSize   = 12.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }

            // ── Stream Quality Presets ────────────────────────────
            SectionLabel("STREAM QUALITY")

            Column(
                modifier            = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color(0xFF161B22)),
                verticalArrangement = Arrangement.spacedBy(0.dp)
            ) {
                StreamPresets.all.forEachIndexed { index, (label, config) ->
                    val isPremiumPreset = label.contains("⭐")
                    val locked         = isPremiumPreset && !isPremium
                    val isSelected     = selected == config
                    val cleanLabel     = label.replace("⭐", "").trim()

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .then(
                                if (isSelected)
                                    Modifier.background(Color(0xFF2979FF).copy(alpha = 0.1f))
                                else Modifier
                            )
                            .clickable(enabled = !locked) {
                                if (!locked) selected = config else onUpgrade()
                            }
                            .padding(horizontal = 16.dp, vertical = 13.dp),
                        verticalAlignment     = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment     = Alignment.CenterVertically
                        ) {
                            // Selection indicator
                            Box(
                                modifier = Modifier
                                    .size(18.dp)
                                    .clip(CircleShape)
                                    .background(
                                        when {
                                            isSelected -> Color(0xFF2979FF)
                                            locked     -> Color(0xFF21262D)
                                            else       -> Color(0xFF21262D)
                                        }
                                    )
                                    .then(
                                        if (!isSelected)
                                            Modifier.border(1.dp, Color(0xFF30363D), CircleShape)
                                        else Modifier
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                if (isSelected) {
                                    Icon(
                                        Icons.Default.Check,
                                        contentDescription = null,
                                        tint     = Color.White,
                                        modifier = Modifier.size(11.dp)
                                    )
                                }
                            }
                            Column {
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    verticalAlignment     = Alignment.CenterVertically
                                ) {
                                    Text(
                                        cleanLabel,
                                        color      = if (locked) Color(0xFF484F58) else Color.White,
                                        fontSize   = 14.sp,
                                        fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
                                    )
                                    if (locked) {
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(4.dp))
                                                .background(Color(0xFFFFD700).copy(alpha = 0.1f))
                                                .padding(horizontal = 5.dp, vertical = 1.dp)
                                        ) {
                                            Text(
                                                "PRO",
                                                color      = Color(0xFFFFD700),
                                                fontSize   = 9.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                }
                                Text(
                                    "${config.width}×${config.height} · ${config.fps}fps · Q${config.quality}",
                                    color    = Color(0xFF8B949E),
                                    fontSize = 12.sp
                                )
                            }
                        }
                    }

                    // Divider between items (not after last)
                    if (index < StreamPresets.all.lastIndex) {
                        HorizontalDivider(
                            color    = Color(0xFF21262D),
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )
                    }
                }
            }

            // ── JPEG Quality Slider ───────────────────────────────
            SectionLabel("JPEG QUALITY")

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape    = RoundedCornerShape(16.dp),
                colors   = CardDefaults.cardColors(containerColor = Color(0xFF161B22)),
                border   = BorderStroke(1.dp, Color(0xFF30363D))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment     = Alignment.CenterVertically
                    ) {
                        Text("Compression Level", color = Color.White, fontSize = 13.sp)
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0xFF2979FF).copy(alpha = 0.12f))
                                .padding(horizontal = 10.dp, vertical = 3.dp)
                        ) {
                            Text(
                                "${selected.quality}%",
                                color      = Color(0xFF2979FF),
                                fontSize   = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                    Spacer(Modifier.height(4.dp))
                    Slider(
                        value         = selected.quality.toFloat(),
                        onValueChange = { selected = selected.copy(quality = it.toInt()) },
                        valueRange    = 20f..90f,
                        colors        = SliderDefaults.colors(
                            thumbColor         = Color(0xFF2979FF),
                            activeTrackColor   = Color(0xFF2979FF),
                            inactiveTrackColor = Color(0xFF21262D)
                        )
                    )
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("20% — faster, smaller", color = Color(0xFF484F58), fontSize = 11.sp)
                        Text("90% — sharper, heavier", color = Color(0xFF484F58), fontSize = 11.sp)
                    }
                }
            }

            // ── Save Button ───────────────────────────────────────
            Button(
                onClick = {
                    scope.launch {
                        userPrefs.saveStreamConfig(selected)
                        if (viewModel.isConnected.value)
                            viewModel.send(BinaryProtocol.streamConfig(selected))
                        onBack()
                    }
                },
                modifier = Modifier.fillMaxWidth().height(54.dp),
                shape    = RoundedCornerShape(14.dp),
                colors   = ButtonDefaults.buttonColors(containerColor = Color(0xFF2979FF))
            ) {
                Icon(
                    Icons.Default.Check,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text("SAVE & APPLY", fontWeight = FontWeight.Bold, fontSize = 15.sp)
            }

            Spacer(Modifier.height(8.dp))
        }
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text,
        color         = Color(0xFF8B949E),
        fontSize      = 10.sp,
        fontWeight    = FontWeight.Bold,
        letterSpacing = 1.sp,
        modifier      = Modifier.padding(horizontal = 2.dp)
    )
}
