package com.tony.pcremote

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch

@Composable
fun AccountTab(
    userPrefs: UserPreferences,
    viewModel: MainViewModel,
    onLogin: () -> Unit,
    onUpgrade: () -> Unit,
    onDisconnect: () -> Unit
) {
    val isPremium by userPrefs.isPremium.collectAsState(initial = false)
    val username by userPrefs.username.collectAsState(initial = "")
    val announcements by viewModel.announcements.collectAsState()
    val isConnected by viewModel.isConnected.collectAsState()
    val scope = rememberCoroutineScope()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        // ── Profile Header ────────────────────────────────────
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(32.dp),
            color = Color(0xFF121212),
            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.05f))
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Box(
                        modifier = Modifier
                            .size(80.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.linearGradient(
                                    listOf(Color(0xFF00E5FF), Color(0xFF7C4DFF))
                                )
                            )
                    )
                    Icon(Icons.Rounded.Person, null, tint = Color.Black, modifier = Modifier.size(40.dp))
                }
                
                Spacer(Modifier.height(16.dp))
                
                Text(
                    if (username.isEmpty()) "Guest User" else username,
                    color = Color.White,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Black
                )
                
                Text(
                    if (isPremium) "Premium Member ⭐" else "Free Account",
                    color = if (isPremium) Color(0xFFFFD700) else Color.White.copy(alpha = 0.4f),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )

                if (username.isEmpty()) {
                    Spacer(Modifier.height(20.dp))
                    Button(
                        onClick = onLogin,
                        modifier = Modifier.fillMaxWidth().height(48.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = Color.Black)
                    ) {
                        Text("Sign In to Sync", fontWeight = FontWeight.Bold)
                    }
                } else if (!isPremium) {
                    Spacer(Modifier.height(20.dp))
                    Button(
                        onClick = onUpgrade,
                        modifier = Modifier.fillMaxWidth().height(48.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFD700), contentColor = Color.Black)
                    ) {
                        Text("Upgrade to Pro", fontWeight = FontWeight.Black)
                    }
                }
            }
        }

        // ── Connection Summary ──────────────────────────────
        Text("ACTIVE SESSIONS", color = Color.White.copy(alpha = 0.4f), fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
        
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            color = Color(0xFF121212),
            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.05f))
        ) {
            Row(
                modifier = Modifier.padding(20.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(if (isConnected) Color(0xFF00C853).copy(alpha = 0.1f) else Color(0xFFFF5252).copy(alpha = 0.1f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        if (isConnected) Icons.Rounded.Wifi else Icons.Rounded.WifiOff, 
                        null, 
                        tint = if (isConnected) Color(0xFF00C853) else Color(0xFFFF5252),
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(Modifier.width(16.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(if (isConnected) "Connected" else "Offline", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    Text(if (isConnected) "Active session on PC" else "No connection active", color = Color.White.copy(alpha = 0.4f), fontSize = 12.sp)
                }
                if (isConnected) {
                    IconButton(onClick = { viewModel.disconnect(); onDisconnect() }) {
                        Icon(Icons.Rounded.PowerSettingsNew, null, tint = Color(0xFFFF5252))
                    }
                }
            }
        }

        // ── Announcements ─────────────────────────────────────
        if (announcements.isNotEmpty()) {
            Text("NOTIFICATIONS", color = Color.White.copy(alpha = 0.4f), fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
            announcements.forEach { ann ->
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    color = Color(0xFF121212).copy(alpha = 0.6f),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.03f))
                ) {
                    Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.Top) {
                        Icon(Icons.Rounded.Info, null, tint = Color(0xFF00E5FF), modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(12.dp))
                        Column {
                            Text(ann.title, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            Text(ann.message, color = Color.White.copy(alpha = 0.5f), fontSize = 11.sp, lineHeight = 16.sp)
                        }
                    }
                }
            }
        }

        // ── Danger Zone ───────────────────────────────────────
        if (username.isNotEmpty()) {
            Spacer(Modifier.height(10.dp))
            OutlinedButton(
                onClick = { scope.launch { userPrefs.logout(); onLogin() } },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, Color(0xFFFF5252).copy(alpha = 0.2f)),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFFF5252))
            ) {
                Text("Logout Account", fontWeight = FontWeight.Bold)
            }
        }
        
        Spacer(Modifier.height(40.dp))
    }
}
