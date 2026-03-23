package com.tony.pcremote

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Computer
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun ConnectScreen(viewModel: MainViewModel, onConnected: () -> Unit) {
    var ip            by remember { mutableStateOf("192.168.1.") }
    val isConnected   by viewModel.isConnected.collectAsState()
    val statusMessage by viewModel.statusMessage.collectAsState()
    val isLoading = statusMessage.contains("Connecting", ignoreCase = true)
    val hasError  = statusMessage.startsWith("Failed") ||
            statusMessage.contains("Error",   ignoreCase = true) ||
            statusMessage.contains("refused", ignoreCase = true)

    LaunchedEffect(isConnected) { if (isConnected) onConnected() }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0D1117))
    ) {
        // Subtle top gradient
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(260.dp)
                .background(
                    Brush.verticalGradient(
                        listOf(Color(0xFF0D2B6B).copy(alpha = 0.4f), Color.Transparent)
                    )
                )
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            // ── App Icon ──────────────────────────────────────────
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(RoundedCornerShape(22.dp))
                    .background(Color(0xFF2979FF).copy(alpha = 0.12f))
                    .border(1.dp, Color(0xFF2979FF).copy(alpha = 0.4f), RoundedCornerShape(22.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.Computer,
                    contentDescription = null,
                    tint     = Color(0xFF2979FF),
                    modifier = Modifier.size(40.dp)
                )
            }

            Spacer(Modifier.height(16.dp))

            Text("PC Remote", fontSize = 26.sp, fontWeight = FontWeight.Bold, color = Color.White)
            Text(
                "Control your PC from your phone",
                fontSize = 14.sp,
                color    = Color(0xFF8B949E),
                modifier = Modifier.padding(top = 4.dp)
            )

            Spacer(Modifier.height(32.dp))

            // ── Connection Card ───────────────────────────────────
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape    = RoundedCornerShape(16.dp),
                colors   = CardDefaults.cardColors(containerColor = Color(0xFF161B22)),
                border   = BorderStroke(1.dp, Color(0xFF30363D))
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.Wifi,
                            contentDescription = null,
                            tint     = Color(0xFF2979FF),
                            modifier = Modifier.size(15.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            "NETWORK CONNECTION",
                            fontSize      = 11.sp,
                            fontWeight    = FontWeight.Bold,
                            color         = Color(0xFF8B949E),
                            letterSpacing = 1.sp
                        )
                    }

                    Spacer(Modifier.height(16.dp))

                    OutlinedTextField(
                        value         = ip,
                        onValueChange = { ip = it },
                        label         = { Text("PC IP Address") },
                        placeholder   = { Text("e.g. 192.168.1.5", color = Color(0xFF484F58)) },
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Number,
                            imeAction    = ImeAction.Done
                        ),
                        keyboardActions = KeyboardActions(
                            onDone = { viewModel.connect(ip.trim()) }
                        ),
                        modifier   = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape      = RoundedCornerShape(10.dp),
                        colors     = OutlinedTextFieldDefaults.colors(
                            focusedTextColor     = Color.White,
                            unfocusedTextColor   = Color.White,
                            focusedBorderColor   = Color(0xFF2979FF),
                            unfocusedBorderColor = Color(0xFF30363D),
                            focusedLabelColor    = Color(0xFF2979FF),
                            unfocusedLabelColor  = Color(0xFF8B949E),
                            cursorColor          = Color(0xFF2979FF)
                        )
                    )

                    // Status indicator
                    AnimatedVisibility(
                        visible = statusMessage.isNotEmpty(),
                        enter   = fadeIn(),
                        exit    = fadeOut()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 12.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(
                                    if (hasError) Color(0xFFFF5252).copy(alpha = 0.08f)
                                    else          Color(0xFF2979FF).copy(alpha = 0.08f)
                                )
                                .padding(horizontal = 12.dp, vertical = 9.dp),
                            verticalAlignment     = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(7.dp)
                                    .clip(CircleShape)
                                    .background(
                                        if (hasError) Color(0xFFFF5252) else Color(0xFF2979FF)
                                    )
                            )
                            Text(
                                statusMessage,
                                color    = if (hasError) Color(0xFFFF5252) else Color(0xFF64B5F6),
                                fontSize = 13.sp
                            )
                        }
                    }

                    Spacer(Modifier.height(16.dp))

                    Button(
                        onClick  = { viewModel.connect(ip.trim()) },
                        modifier = Modifier.fillMaxWidth().height(52.dp),
                        shape    = RoundedCornerShape(12.dp),
                        colors   = ButtonDefaults.buttonColors(
                            containerColor         = Color(0xFF2979FF),
                            disabledContainerColor = Color(0xFF2979FF).copy(alpha = 0.4f)
                        ),
                        enabled = !isLoading
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(
                                color       = Color.White,
                                modifier    = Modifier.size(18.dp),
                                strokeWidth = 2.dp
                            )
                            Spacer(Modifier.width(10.dp))
                            Text("Connecting...", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        } else {
                            Icon(
                                Icons.Default.Computer,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(Modifier.width(8.dp))
                            Text("CONNECT", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        }
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            // ── Hint Card ─────────────────────────────────────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFF161B22))
                    .border(1.dp, Color(0xFF30363D), RoundedCornerShape(12.dp))
                    .padding(14.dp),
                verticalAlignment     = Alignment.Top,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text("💡", fontSize = 17.sp)
                Column {
                    Text(
                        "How to find your PC IP",
                        color      = Color.White,
                        fontSize   = 13.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(Modifier.height(3.dp))
                    Text(
                        "Open cmd on your PC → type ipconfig\nLook for IPv4 Address under Wi-Fi",
                        color      = Color(0xFF8B949E),
                        fontSize   = 12.sp,
                        lineHeight = 18.sp
                    )
                }
            }

            Spacer(Modifier.height(14.dp))

            Text(
                "Both devices must be on the same Wi-Fi network",
                color     = Color(0xFF484F58),
                fontSize  = 12.sp,
                textAlign = TextAlign.Center
            )
        }
    }
}
