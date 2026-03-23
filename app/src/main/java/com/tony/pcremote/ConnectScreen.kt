package com.tony.pcremote

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun ConnectScreen(
    viewModel:   MainViewModel,
    onConnected: () -> Unit,
    onOpenLogin: () -> Unit
) {
    var ip            by remember { mutableStateOf("") }
    val isConnected   by viewModel.isConnected.collectAsState()
    val statusMessage by viewModel.statusMessage.collectAsState()
    val discovered    by viewModel.discoveredDevices.collectAsState()
    
    val isLoading = statusMessage.contains("Connecting", ignoreCase = true)
    val hasError  = statusMessage.startsWith("Failed") || statusMessage.contains("Error", ignoreCase = true)

    // Only trigger auto-navigation if the connection was established while on this screen
    var connectionAttemptActive by remember { mutableStateOf(false) }

    LaunchedEffect(isConnected) { 
        if (isConnected && connectionAttemptActive) {
            connectionAttemptActive = false
            onConnected() 
        }
    }
    
    DisposableEffect(Unit) {
        viewModel.startDiscovery()
        onDispose { viewModel.stopDiscovery() }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        // ── Discovery Section ─────────────────────────────────
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    "Nearby Devices",
                    style = MaterialTheme.typography.titleSmall.copy(
                        color = Color.White.copy(alpha = 0.6f),
                        fontWeight = FontWeight.Bold
                    )
                )
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        color = Color(0xFF00E5FF),
                        strokeWidth = 2.dp
                    )
                }
            }

            if (discovered.isEmpty() && !isLoading) {
                Surface(
                    modifier = Modifier.fillMaxWidth().height(100.dp),
                    shape = RoundedCornerShape(24.dp),
                    color = Color(0xFF121212),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.05f))
                ) {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(Icons.Rounded.Search, null, tint = Color.White.copy(alpha = 0.2f))
                        Text("Searching for PCs...", color = Color.White.copy(alpha = 0.3f), fontSize = 12.sp)
                    }
                }
            } else {
                discovered.forEach { device ->
                    DeviceCard(device.name, device.ip) { 
                        connectionAttemptActive = true
                        viewModel.connect(device.ip, device.name) 
                    }
                }
            }
        }

        // ── Manual Entry Section ──────────────────────────────
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(32.dp),
            color = Color(0xFF121212),
            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f))
        ) {
            Column(modifier = Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text(
                    "Manual Connection",
                    color = Color.White,
                    fontWeight = FontWeight.Black,
                    fontSize = 18.sp
                )
                
                OutlinedTextField(
                    value = ip,
                    onValueChange = { ip = it },
                    placeholder = { Text("Enter IP Address", color = Color.White.copy(alpha = 0.2f)) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = Color(0xFF00E5FF),
                        unfocusedBorderColor = Color.White.copy(alpha = 0.1f),
                        cursorColor = Color(0xFF00E5FF),
                        focusedContainerColor = Color(0xFF080808),
                        unfocusedContainerColor = Color(0xFF080808)
                    ),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = { 
                        if (ip.isNotBlank()) {
                            connectionAttemptActive = true
                            viewModel.connect(ip.trim(), "Manual PC")
                        }
                    })
                )

                if (statusMessage.isNotEmpty() && !isLoading) {
                    Text(
                        text = statusMessage,
                        color = if (hasError) Color(0xFFFF5252) else Color(0xFF00E5FF),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                Button(
                    onClick = { 
                        if (ip.isNotBlank()) {
                            connectionAttemptActive = true
                            viewModel.connect(ip.trim(), "Manual PC")
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF00E5FF),
                        contentColor = Color.Black
                    ),
                    enabled = !isLoading
                ) {
                    Text("Connect to PC", fontWeight = FontWeight.Black, fontSize = 16.sp)
                }
            }
        }
        
        Spacer(Modifier.weight(1f))
        
        Text(
            "Make sure both devices are on the same Wi-Fi network and the PC Remote Server is running.",
            color = Color.White.copy(alpha = 0.3f),
            fontSize = 11.sp,
            textAlign = TextAlign.Center,
            lineHeight = 16.sp,
            modifier = Modifier.padding(bottom = 16.dp)
        )
    }
}

@Composable
fun DeviceCard(name: String, ip: String, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        color = Color(0xFF161616),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.05f))
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF00E5FF).copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Rounded.DesktopWindows, null, tint = Color(0xFF00E5FF), modifier = Modifier.size(24.dp))
            }
            Spacer(Modifier.width(16.dp))
            Column {
                Text(name, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                Text(ip, color = Color.White.copy(alpha = 0.4f), fontSize = 12.sp)
            }
            Spacer(Modifier.weight(1f))
            Icon(Icons.Rounded.ChevronRight, null, tint = Color.White.copy(alpha = 0.2f))
        }
    }
}
