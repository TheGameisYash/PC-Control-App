package com.tony.pcremote

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

@Composable
fun LicenseScreen(
    userPrefs:   UserPreferences,
    onActivated: () -> Unit,
    onBack:      () -> Unit
) {
    val scope     = rememberCoroutineScope()
    var key       by remember { mutableStateOf("") }
    var error     by remember { mutableStateOf("") }
    var success   by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }
    var statusMsg by remember { mutableStateOf("") }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0D1117))
    ) {
        // Gold gradient top
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(220.dp)
                .background(
                    Brush.verticalGradient(
                        listOf(Color(0xFFFFD700).copy(alpha = 0.07f), Color.Transparent)
                    )
                )
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 22.dp, vertical = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            // Back button
            Row(modifier = Modifier.fillMaxWidth()) {
                TextButton(
                    onClick        = onBack,
                    contentPadding = PaddingValues(0.dp)
                ) {
                    Text("← Back", color = Color(0xFF8B949E), fontSize = 13.sp)
                }
            }

            Spacer(Modifier.height(12.dp))

            // Star icon
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color(0xFFFFD700).copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) { Text("⭐", fontSize = 34.sp) }

            Spacer(Modifier.height(14.dp))

            Text(
                "Activate Premium",
                fontSize   = 24.sp,
                fontWeight = FontWeight.Bold,
                color      = Color(0xFFFFD700)
            )
            Text(
                "Unlock the full PC Remote experience",
                color    = Color(0xFF8B949E),
                fontSize = 13.sp,
                modifier = Modifier.padding(top = 4.dp),
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.height(20.dp))

            // ── Features Card ─────────────────────────────────────
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape    = RoundedCornerShape(16.dp),
                colors   = CardDefaults.cardColors(containerColor = Color(0xFF161B22)),
                border   = BorderStroke(1.dp, Color(0xFFFFD700).copy(alpha = 0.2f))
            ) {
                Column(
                    modifier            = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        "What you get",
                        color      = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize   = 14.sp
                    )
                    listOf(
                        "🖥️" to "Full Remote Desktop mode",
                        "📺" to "1080p / 60fps screen streaming",
                        "⚡" to "Ultra quality stream preset",
                        "🔑" to "Account-linked — works on any device"
                    ).forEach { (icon, text) ->
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment     = Alignment.CenterVertically
                        ) {
                            Text(icon, fontSize = 16.sp)
                            Text(text, color = Color(0xFF8B949E), fontSize = 13.sp)
                        }
                    }
                }
            }

            Spacer(Modifier.height(20.dp))

            // ── Key Input ─────────────────────────────────────────
            OutlinedTextField(
                value         = key,
                onValueChange = { key = it.uppercase() },
                label         = { Text("License Key") },
                placeholder   = { Text("LIC-XXXX-XXXX-XXXX", color = Color(0xFF484F58)) },
                modifier      = Modifier.fillMaxWidth(),
                shape         = RoundedCornerShape(12.dp),
                colors        = OutlinedTextFieldDefaults.colors(
                    focusedTextColor     = Color.White,
                    unfocusedTextColor   = Color.White,
                    focusedBorderColor   = Color(0xFFFFD700),
                    unfocusedBorderColor = Color(0xFF30363D),
                    focusedLabelColor    = Color(0xFFFFD700),
                    unfocusedLabelColor  = Color(0xFF8B949E),
                    cursorColor          = Color(0xFFFFD700)
                ),
                singleLine = true
            )

            // Messages
            if (error.isNotEmpty()) {
                Spacer(Modifier.height(10.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFFFF5252).copy(alpha = 0.08f))
                        .padding(horizontal = 12.dp, vertical = 9.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment     = Alignment.CenterVertically
                ) {
                    Text("❌", fontSize = 13.sp)
                    Text(error, color = Color(0xFFFF5252), fontSize = 13.sp)
                }
            }
            if (success) {
                Spacer(Modifier.height(10.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFF00C853).copy(alpha = 0.08f))
                        .padding(horizontal = 12.dp, vertical = 9.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment     = Alignment.CenterVertically
                ) {
                    Text("✅", fontSize = 13.sp)
                    Text(
                        "Premium activated successfully!",
                        color      = Color(0xFF00C853),
                        fontSize   = 13.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            Spacer(Modifier.height(20.dp))

            // ── Activate Button ───────────────────────────────────
            Button(
                onClick = {
                    if (key.isBlank()) { error = "Please enter your license key"; return@Button }
                    isLoading = true; error = ""; statusMsg = "Validating license..."
                    scope.launch {
                        val username = userPrefs.username.first()
                        if (username.isBlank()) {
                            error = "Please login first before activating"
                            isLoading = false; statusMsg = ""; return@launch
                        }
                        val sanitizedUser = username.trim().lowercase().replace(Regex("[^a-z0-9]"), "")
                        if (sanitizedUser.isBlank()) {
                            error = "Invalid username. Please use a valid account."
                            isLoading = false; statusMsg = ""; return@launch
                        }
                        statusMsg = "Binding to account: $sanitizedUser..."
                        val result = LicenseManager.activateLicense(key, username)
                        isLoading = false; statusMsg = ""
                        if (result.success) {
                            userPrefs.activateLicenseLocal(key)
                            success = true
                            kotlinx.coroutines.delay(1500)
                            onActivated()
                        } else {
                            error = when (result.code) {
                                "ALREADY_REGISTERED" -> "License already bound to another account"
                                "INVALID_LICENSE"    -> "Invalid license key"
                                "EXPIRED"            -> "License has expired"
                                "MAX_DEVICES"        -> "Max devices reached for this license"
                                else                 -> result.message
                            }
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth().height(54.dp),
                shape    = RoundedCornerShape(14.dp),
                colors   = ButtonDefaults.buttonColors(
                    containerColor         = Color(0xFFFFD700),
                    disabledContainerColor = Color(0xFFFFD700).copy(alpha = 0.4f)
                ),
                enabled = !isLoading
            ) {
                if (isLoading) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment     = Alignment.CenterVertically
                    ) {
                        CircularProgressIndicator(
                            color       = Color.Black,
                            modifier    = Modifier.size(18.dp),
                            strokeWidth = 2.dp
                        )
                        Text(
                            statusMsg.ifEmpty { "Activating..." },
                            color      = Color.Black,
                            fontSize   = 14.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                } else {
                    Text(
                        "ACTIVATE LICENSE",
                        color      = Color.Black,
                        fontWeight = FontWeight.Bold,
                        fontSize   = 15.sp
                    )
                }
            }
        }
    }
}
