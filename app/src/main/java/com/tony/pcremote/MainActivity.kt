package com.tony.pcremote

import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        // ✅ High Refresh Rate Optimization
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            window.attributes.preferredDisplayModeId = 0 
        } else {
            @Suppress("DEPRECATION")
            val modes = window.windowManager.defaultDisplay.supportedModes
            val bestMode = modes.maxByOrNull { it.refreshRate }
            if (bestMode != null) {
                window.attributes.preferredRefreshRate = bestMode.refreshRate
            }
        }

        val userPrefs = UserPreferences(applicationContext)

        setContent {
            MaterialTheme {
                Surface(color = Color(0xFF050505)) {
                    AppRoot(
                        viewModel   = viewModel,
                        userPrefs   = userPrefs
                    )
                }
            }
        }
    }
}

@Composable
fun AppRoot(
    viewModel:   MainViewModel,
    userPrefs:   UserPreferences
) {
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

    MainScreen(
        viewModel    = viewModel,
        userPrefs    = userPrefs,
        onDisconnect = { /* Handled within MainScreen connection tab */ }
    )
}
