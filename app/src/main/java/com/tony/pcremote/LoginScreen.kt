package com.tony.pcremote

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(
    userPrefs:  UserPreferences,
    onLoggedIn: () -> Unit,
    onBack:     () -> Unit
) {
    var isLoginTab  by remember { mutableStateOf(true) }
    var username    by remember { mutableStateOf("") }
    var password    by remember { mutableStateOf("") }
    var licenseKey  by remember { mutableStateOf("") }
    var error       by remember { mutableStateOf("") }
    var isLoading   by remember { mutableStateOf(false) }
    val scope       = rememberCoroutineScope()

    Scaffold(
        containerColor = Color(0xFF050505),
        topBar = {
            TopAppBar(
                title = { },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, null, tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize()) {
            // Background Glow
            Box(
                modifier = Modifier
                    .size(400.dp)
                    .align(Alignment.TopEnd)
                    .offset(x = 100.dp, y = (-100).dp)
                    .background(Brush.radialGradient(listOf(Color(0xFF00E5FF).copy(alpha = 0.15f), Color.Transparent)))
            )

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 32.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(Modifier.height(20.dp))
                
                // App Logo/Icon
                Surface(
                    modifier = Modifier.size(80.dp),
                    shape = RoundedCornerShape(24.dp),
                    color = Color(0xFF121212),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.1f))
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(Icons.Rounded.Terminal, null, tint = Color(0xFF00E5FF), modifier = Modifier.size(40.dp))
                    }
                }

                Spacer(Modifier.height(32.dp))
                
                Text(
                    text = if (isLoginTab) "Welcome Back" else "Join PRO",
                    style = MaterialTheme.typography.headlineLarge.copy(
                        fontWeight = FontWeight.Black,
                        color = Color.White,
                        letterSpacing = (-1.5).sp
                    )
                )
                
                Text(
                    text = if (isLoginTab) "Sign in to your account" else "Create an account to sync settings",
                    color = Color.White.copy(alpha = 0.4f),
                    fontSize = 14.sp
                )

                Spacer(Modifier.height(48.dp))

                // ── Auth Fields ──────────────────────────────────────
                ModernAuthField(
                    value = username,
                    onValueChange = { username = it },
                    label = "Username",
                    icon = Icons.Rounded.Person
                )
                
                Spacer(Modifier.height(16.dp))
                
                if (!isLoginTab) {
                    ModernAuthField(
                        value = licenseKey,
                        onValueChange = { licenseKey = it.trim().uppercase() },
                        label = "License Key (Optional)",
                        icon = Icons.Rounded.VpnKey
                    )
                    Spacer(Modifier.height(16.dp))
                }

                ModernAuthField(
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

                Spacer(Modifier.height(48.dp))

                // ── Action Button ────────────────────────────────────
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
                                onLoggedIn()
                            } else {
                                error = result.message
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(60.dp),
                    shape = RoundedCornerShape(20.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00E5FF), contentColor = Color.Black),
                    enabled = !isLoading
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(color = Color.Black, modifier = Modifier.size(24.dp), strokeWidth = 3.dp)
                    } else {
                        Text(if (isLoginTab) "Sign In" else "Create Account", fontWeight = FontWeight.Black, fontSize = 16.sp)
                    }
                }

                Spacer(Modifier.height(24.dp))

                TextButton(onClick = { isLoginTab = !isLoginTab; error = "" }) {
                    Text(
                        if (isLoginTab) "Don't have an account? Sign Up" else "Already have an account? Sign In",
                        color = Color.White.copy(alpha = 0.5f),
                        fontSize = 13.sp
                    )
                }
            }
        }
    }
}

@Composable
fun ModernAuthField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    isPassword: Boolean = false
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        leadingIcon = { Icon(icon, null, tint = Color(0xFF00E5FF).copy(alpha = 0.6f)) },
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
            focusedContainerColor = Color(0xFF080808),
            unfocusedContainerColor = Color(0xFF080808)
        ),
        singleLine = true
    )
}
