package com.tony.pcremote

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch

@Composable
fun AccountTab(
    userPrefs: UserPreferences,
    viewModel: MainViewModel,
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
        if (username.isEmpty()) {
            // ── Auth Section for Guests ───────────────────────────
            AuthSection(userPrefs)
        } else {
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
                        username,
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

                    if (!isPremium) {
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
            Spacer(Modifier.height(10.dp))
            OutlinedButton(
                onClick = { scope.launch { userPrefs.logout() } },
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

@Composable
fun AuthSection(userPrefs: UserPreferences) {
    var isLoginTab  by remember { mutableStateOf(true) }
    var username    by remember { mutableStateOf("") }
    var password    by remember { mutableStateOf("") }
    var licenseKey  by remember { mutableStateOf("") }
    var error       by remember { mutableStateOf("") }
    var isLoading   by remember { mutableStateOf(false) }
    val scope       = rememberCoroutineScope()

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
            Text(
                text = if (isLoginTab) "Welcome Back" else "Create Account",
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.Black,
                    color = Color.White,
                    letterSpacing = (-1).sp
                )
            )
            
            Text(
                text = if (isLoginTab) "Sign in to sync your settings" else "Join us to unlock cloud features",
                color = Color.White.copy(alpha = 0.4f),
                fontSize = 12.sp,
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.height(24.dp))

            ModernAccountField(
                value = username,
                onValueChange = { username = it },
                label = "Username",
                icon = Icons.Rounded.Person
            )
            
            Spacer(Modifier.height(12.dp))
            
            if (!isLoginTab) {
                ModernAccountField(
                    value = licenseKey,
                    onValueChange = { licenseKey = it.trim().uppercase() },
                    label = "License Key (Optional)",
                    icon = Icons.Rounded.VpnKey
                )
                Spacer(Modifier.height(12.dp))
            }

            ModernAccountField(
                value = password,
                onValueChange = { password = it },
                label = "Password",
                icon = Icons.Rounded.Lock,
                isPassword = true
            )

            if (error.isNotEmpty()) {
                Text(
                    text = error,
                    color = Color(0xFFFF5252),
                    fontSize = 12.sp,
                    modifier = Modifier.padding(top = 16.dp),
                    textAlign = TextAlign.Center
                )
            }

            Spacer(Modifier.height(24.dp))

            Button(
                onClick = {
                    if (username.isBlank() || password.isBlank()) {
                        error = "Required fields missing"; return@Button
                    }
                    isLoading = true
                    scope.launch {
                        val result = if (isLoginTab) 
                            AuthManager.login(username, password)
                        else 
                            AuthManager.register(username, password, licenseKey, "")

                        isLoading = false
                        if (result.success) {
                            userPrefs.saveUserSession(username, password, result.data?.isPremium ?: false)
                        } else {
                            error = result.message
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF00E5FF), 
                    contentColor = Color.Black
                ),
                enabled = !isLoading
            ) {
                if (isLoading) {
                    CircularProgressIndicator(color = Color.Black, modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                } else {
                    Text(if (isLoginTab) "Sign In" else "Sign Up", fontWeight = FontWeight.Bold)
                }
            }

            Spacer(Modifier.height(16.dp))

            TextButton(onClick = { isLoginTab = !isLoginTab; error = "" }) {
                Text(
                    if (isLoginTab) "New here? Create an account" else "Already have an account? Sign In",
                    color = Color.White.copy(alpha = 0.5f),
                    fontSize = 12.sp
                )
            }
        }
    }
}

@Composable
fun ModernAccountField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    isPassword: Boolean = false
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label, fontSize = 13.sp) },
        leadingIcon = { Icon(icon, null, tint = Color(0xFF00E5FF).copy(alpha = 0.6f), modifier = Modifier.size(18.dp)) },
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        visualTransformation = if (isPassword) PasswordVisualTransformation() else androidx.compose.ui.text.input.VisualTransformation.None,
        keyboardOptions = KeyboardOptions(
            keyboardType = if (isPassword) KeyboardType.Password else KeyboardType.Text,
            imeAction = ImeAction.Next
        ),
        colors = OutlinedTextFieldDefaults.colors(
            focusedTextColor = Color.White,
            unfocusedTextColor = Color.White,
            focusedBorderColor = Color(0xFF00E5FF),
            unfocusedBorderColor = Color.White.copy(alpha = 0.1f),
            focusedLabelColor = Color(0xFF00E5FF),
            unfocusedLabelColor = Color.White.copy(alpha = 0.3f),
            cursorColor = Color(0xFF00E5FF),
            focusedContainerColor = Color(0xFF1A1A1A),
            unfocusedContainerColor = Color(0xFF1A1A1A)
        ),
        singleLine = true
    )
}
