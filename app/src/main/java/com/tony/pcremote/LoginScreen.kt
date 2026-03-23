package com.tony.pcremote

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch

@Composable
fun LoginScreen(
    userPrefs:  UserPreferences,
    onLoggedIn: () -> Unit,
    onSkip:     () -> Unit
) {
    var isLoginTab  by remember { mutableStateOf(true) }
    var username    by remember { mutableStateOf("") }
    var password    by remember { mutableStateOf("") }
    var email       by remember { mutableStateOf("") }
    var licenseKey  by remember { mutableStateOf("") }   // ← NEW
    var error       by remember { mutableStateOf("") }
    var statusMsg   by remember { mutableStateOf("") }
    var isLoading   by remember { mutableStateOf(false) }
    val scope       = rememberCoroutineScope()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0D1117))
            .padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("🖥️ PC Remote", fontSize = 30.sp,
            fontWeight = FontWeight.Bold, color = Color.White)
        Spacer(Modifier.height(6.dp))
        Text("Sign in to your account", color = Color(0xFF8B949E), fontSize = 14.sp)
        Spacer(Modifier.height(32.dp))

        // ── Tab Toggle ────────────────────────────────────────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF161B22), RoundedCornerShape(10.dp))
                .padding(4.dp)
        ) {
            listOf("Login", "Register").forEachIndexed { i, label ->
                val selected = (i == 0) == isLoginTab
                Button(
                    onClick = {
                        isLoginTab = (i == 0)
                        error      = ""
                        statusMsg  = ""
                        licenseKey = ""   // clear on tab switch
                    },
                    modifier  = Modifier.weight(1f),
                    shape     = RoundedCornerShape(8.dp),
                    colors    = ButtonDefaults.buttonColors(
                        containerColor = if (selected) Color(0xFF2979FF) else Color.Transparent
                    ),
                    elevation = null
                ) { Text(label, color = Color.White) }
            }
        }

        Spacer(Modifier.height(24.dp))

        // ── Username ──────────────────────────────────────────
        OutlinedTextField(
            value         = username,
            onValueChange = { username = it },
            label         = { Text("Username", color = Color.Gray) },
            modifier      = Modifier.fillMaxWidth(),
            singleLine    = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
            colors        = loginFieldColors()
        )
        Spacer(Modifier.height(10.dp))

        // ── Register-only fields ──────────────────────────────
        if (!isLoginTab) {

            // License Key — required for registration
            OutlinedTextField(
                value         = licenseKey,
                onValueChange = { licenseKey = it.trim().uppercase() },
                label         = { Text("License Key", color = Color.Gray) },
                placeholder   = { Text("e.g. PC123", color = Color(0xFF4A5568)) },
                leadingIcon   = {
                    Icon(
                        imageVector        = Icons.Default.Lock,
                        contentDescription = null,
                        tint               = Color(0xFF2979FF)
                    )
                },
                modifier        = Modifier.fillMaxWidth(),
                singleLine      = true,
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Characters,
                    imeAction      = ImeAction.Next
                ),
                colors = loginFieldColors()
            )
            Spacer(Modifier.height(10.dp))

            // Email (optional)
            OutlinedTextField(
                value           = email,
                onValueChange   = { email = it },
                label           = { Text("Email (optional)", color = Color.Gray) },
                modifier        = Modifier.fillMaxWidth(),
                singleLine      = true,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Email,
                    imeAction    = ImeAction.Next
                ),
                colors = loginFieldColors()
            )
            Spacer(Modifier.height(10.dp))
        }

        // ── Password ──────────────────────────────────────────
        OutlinedTextField(
            value                = password,
            onValueChange        = { password = it },
            label                = { Text("Password", color = Color.Gray) },
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions      = KeyboardOptions(
                keyboardType = KeyboardType.Password,
                imeAction    = ImeAction.Done
            ),
            modifier   = Modifier.fillMaxWidth(),
            singleLine = true,
            colors     = loginFieldColors()
        )

        // ── Messages ──────────────────────────────────────────
        if (error.isNotEmpty()) {
            Spacer(Modifier.height(10.dp))
            Text(error, color = Color(0xFFFF5252), fontSize = 13.sp)
        }
        if (statusMsg.isNotEmpty()) {
            Spacer(Modifier.height(10.dp))
            Text(statusMsg, color = Color(0xFF64B5F6), fontSize = 13.sp)
        }

        Spacer(Modifier.height(20.dp))

        // ── Submit Button ─────────────────────────────────────
        Button(
            onClick = {
                // Client-side validation
                if (username.isBlank() || password.isBlank()) {
                    error = "Please fill all fields"; return@Button
                }
                if (password.length < 4) {
                    error = "Password must be at least 4 characters"; return@Button
                }
                if (!isLoginTab && licenseKey.isBlank()) {
                    error = "❌ License key is required to register"; return@Button
                }

                isLoading = true; error = ""
                statusMsg = if (isLoginTab) "Logging in..." else "Creating account..."

                scope.launch {
                    val result = if (isLoginTab)
                        AuthManager.login(username,password)
                    else
                        AuthManager.register(
                            username   = username,
                            password   = password,
                            licenseKey = licenseKey,  // ← passed here
                            email      = email
                        )

                    isLoading = false; statusMsg = ""

                    if (result.success) {
                        userPrefs.saveUserSession(
                            username  = username.trim().lowercase(),
                            password  = password,
                            isPremium = result.data?.isPremium ?: false
                        )
                        onLoggedIn()
                    } else {
                        error = when (result.code) {
                            "MISSING_FIELDS"       -> "❌ Please fill all required fields"
                            "INVALID_USERNAME"     -> "❌ Letters, numbers and underscores only"
                            "USERNAME_TOO_SHORT"   -> "❌ Username must be 3+ characters"
                            "USERNAME_TOO_LONG"    -> "❌ Username too long (max 20)"
                            "PASSWORD_TOO_SHORT"   -> "❌ Password must be 4+ characters"
                            "PASSWORD_TOO_LONG"    -> "❌ Password too long (max 64)"
                            "LICENSE_REQUIRED"     -> "❌ License key is required to register"
                            "INVALID_LICENSE_FORMAT"-> "❌ Invalid license key format"
                            "LICENSE_NOT_FOUND"    -> "❌ License key not found"
                            "LICENSE_BANNED"       -> "❌ This license has been banned"
                            "LICENSE_EXPIRED"      -> "❌ This license has expired"
                            "LICENSE_ALREADY_USED" -> "❌ License already linked to another account"
                            "USER_EXISTS"          -> "❌ Username already taken"
                            "INVALID_CREDENTIALS"  -> "❌ Wrong username or password"
                            "ACCOUNT_BANNED"       -> "❌ Account suspended. Contact support."
                            "RATE_LIMITED"         -> "⏳ Too many attempts. Wait 1 minute."
                            "TIMEOUT"              -> "⏱️ Connection timed out. Try again."
                            "NO_INTERNET"          -> "📶 No internet connection"
                            "SERVER_DOWN"          -> "🔧 Server unavailable. Try later."
                            "SERVER_ERROR"         -> "⚠️ Server error. Try again."
                            else                   -> "❌ ${result.message}"
                        }

                    }
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            shape    = RoundedCornerShape(12.dp),
            colors   = ButtonDefaults.buttonColors(containerColor = Color(0xFF2979FF)),
            enabled  = !isLoading
        ) {
            if (isLoading) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment     = Alignment.CenterVertically
                ) {
                    CircularProgressIndicator(
                        color     = Color.White,
                        modifier  = Modifier.size(18.dp),
                        strokeWidth = 2.dp
                    )
                    Text(statusMsg.ifEmpty { "Please wait..." }, color = Color.White)
                }
            } else {
                Text(
                    if (isLoginTab) "LOGIN" else "CREATE ACCOUNT",
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Spacer(Modifier.height(16.dp))
        TextButton(onClick = onSkip) {
            Text("Skip for now →", color = Color(0xFF8B949E), fontSize = 13.sp)
        }
    }
}

@Composable
private fun loginFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedTextColor     = Color.White,
    unfocusedTextColor   = Color.White,
    focusedBorderColor   = Color(0xFF2979FF),
    unfocusedBorderColor = Color(0xFF2D333B)
)
