package com.tony.pcremote

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val userPrefs   = UserPreferences(applicationContext)
        val wasLoggedIn = runBlocking { userPrefs.isLoggedIn.first() }

        setContent {
            MaterialTheme {
                AppRoot(
                    viewModel   = viewModel,
                    userPrefs   = userPrefs,
                    startScreen = if (wasLoggedIn) "connect" else "login"
                )
            }
        }
    }
}

@Composable
fun AppRoot(
    viewModel:   MainViewModel,
    userPrefs:   UserPreferences,
    startScreen: String
) {
    var screen      by remember { mutableStateOf(startScreen) }
    val isConnected by viewModel.isConnected.collectAsState()
    var updateInfo  by remember { mutableStateOf<UpdateInfo?>(null) }

    LaunchedEffect(Unit) {
        updateInfo = UpdateManager.checkForUpdate(BuildConfig.VERSION_CODE)
    }

    updateInfo?.let { info ->
        UpdateDialog(
            info      = info,
            onDismiss = { updateInfo = null }
        )
    }

    when (screen) {
        "login" -> LoginScreen(
            userPrefs  = userPrefs,
            onLoggedIn = { screen = "connect" },
            onSkip     = { screen = "connect" }
        )
        "connect" -> ConnectScreen(
            viewModel   = viewModel,
            onConnected = { screen = "main" }
        )
        "main" -> {
            if (!isConnected) {
                screen = "connect"
            } else {
                MainScreen(
                    viewModel    = viewModel,
                    userPrefs    = userPrefs,
                    onDisconnect = { screen = "connect" },
                    onLoggedOut  = { screen = "login" }
                )
            }
        }
    }
}
