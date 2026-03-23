package com.tony.pcremote

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Computer
import androidx.compose.material.icons.filled.Keyboard
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private data class NavTab(val icon: ImageVector, val label: String)

@OptIn(ExperimentalAnimationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    viewModel:    MainViewModel,
    userPrefs:    UserPreferences,
    onDisconnect: () -> Unit,
    onLoggedOut:  () -> Unit
) {
    val isPremium     by userPrefs.isPremium.collectAsState(initial = false)
    val username      by userPrefs.username.collectAsState(initial = "")
    val announcements by viewModel.announcements.collectAsState()

    LaunchedEffect(Unit) { viewModel.fetchAnnouncements() }

    var selectedTab  by remember { mutableStateOf(0) }
    var showSettings by remember { mutableStateOf(false) }
    var showLicense  by remember { mutableStateOf(false) }

    // Track dismissed announcements
    val dismissed = remember { mutableStateListOf<String>() }

    if (showSettings) {
        SettingsScreen(
            userPrefs   = userPrefs,
            isPremium   = isPremium,
            viewModel   = viewModel,
            onBack      = { showSettings = false },
            onUpgrade   = { showSettings = false; showLicense = true },
            onLoggedOut = { showSettings = false; viewModel.disconnect(); onLoggedOut() }
        )
        return
    }

    if (showLicense) {
        LicenseScreen(
            userPrefs   = userPrefs,
            onActivated = { showLicense = false },
            onBack      = { showLicense = false }
        )
        return
    }

    val navTabs = listOf(
        NavTab(Icons.Default.TouchApp, "Touchpad"),
        NavTab(Icons.Default.Computer, "Desktop"),
        NavTab(Icons.Default.Keyboard, "Keys"),
        NavTab(Icons.Default.MusicNote, "Media")
    )

    val isDesktopTab = selectedTab == 1

    Scaffold(
        containerColor = Color(0xFF0D1117),
        topBar = {
            if (!isDesktopTab) {
                TopAppBar(
                    title = {
                        Row(
                            verticalAlignment  = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            // Username avatar circle
                            if (username.isNotEmpty()) {
                                Box(
                                    modifier = Modifier
                                        .size(32.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFF2979FF).copy(alpha = 0.2f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        username.first().uppercaseChar().toString(),
                                        color      = Color(0xFF2979FF),
                                        fontSize   = 14.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                            Column {
                                Row(
                                    verticalAlignment     = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                                ) {
                                    Text(
                                        "PC Remote",
                                        color      = Color.White,
                                        fontWeight = FontWeight.Bold,
                                        fontSize   = 16.sp
                                    )
                                    if (isPremium) {
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(4.dp))
                                                .background(Color(0xFFFFD700).copy(alpha = 0.15f))
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
                                if (username.isNotEmpty()) {
                                    Text(
                                        username,
                                        color    = Color(0xFF8B949E),
                                        fontSize = 11.sp
                                    )
                                }
                            }
                        }
                    },
                    actions = {
                        if (!isPremium) {
                            TextButton(
                                onClick = { showLicense = true },
                                contentPadding = PaddingValues(horizontal = 8.dp)
                            ) {
                                Icon(
                                    Icons.Default.Star,
                                    contentDescription = null,
                                    tint     = Color(0xFFFFD700),
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(Modifier.width(4.dp))
                                Text(
                                    "Upgrade",
                                    color      = Color(0xFFFFD700),
                                    fontSize   = 12.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                        IconButton(onClick = { showSettings = true }) {
                            Icon(
                                Icons.Default.Settings,
                                contentDescription = "Settings",
                                tint = Color(0xFF8B949E)
                            )
                        }
                        IconButton(
                            onClick = { viewModel.disconnect(); onDisconnect() }
                        ) {
                            Icon(
                                Icons.Default.Close,
                                contentDescription = "Disconnect",
                                tint = Color(0xFFFF5252)
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color(0xFF161B22)
                    )
                )
            }
        },
        bottomBar = {
            if (!isDesktopTab) {
                NavigationBar(
                    containerColor = Color(0xFF161B22),
                    tonalElevation = 0.dp
                ) {
                    navTabs.forEachIndexed { index, tab ->
                        val selected = selectedTab == index
                        NavigationBarItem(
                            selected = selected,
                            onClick  = { selectedTab = index },
                            icon = {
                                Icon(
                                    tab.icon,
                                    contentDescription = tab.label,
                                    modifier = Modifier.size(22.dp)
                                )
                            },
                            label = {
                                Text(tab.label, fontSize = 10.sp, fontWeight = FontWeight.Medium)
                            },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor   = Color(0xFF2979FF),
                                selectedTextColor   = Color(0xFF2979FF),
                                unselectedIconColor = Color(0xFF8B949E),
                                unselectedTextColor = Color(0xFF8B949E),
                                indicatorColor      = Color(0xFF2979FF).copy(alpha = 0.12f)
                            )
                        )
                    }
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .background(Color(0xFF0D1117))
        ) {

            // ── Announcement Banners ──────────────────────────────
            if (!isDesktopTab) {
                announcements
                    .filter { it.title !in dismissed }
                    .forEach { ann ->
                        val (bgColor, borderColor) = when (ann.type) {
                            "critical" -> Color(0xFF7F0000) to Color(0xFFFF5252)
                            "warning"  -> Color(0xFF4A2000) to Color(0xFFFF9100)
                            "offer"    -> Color(0xFF1A3D00) to Color(0xFF00C853)
                            else       -> Color(0xFF0D2B6B) to Color(0xFF2979FF)
                        }
                        val emoji = when (ann.type) {
                            "critical" -> "🚨"
                            "warning"  -> "⚠️"
                            "offer"    -> "🎉"
                            else       -> "📢"
                        }
                        AnimatedVisibility(
                            visible = ann.title !in dismissed,
                            enter   = fadeIn(),
                            exit    = fadeOut()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 4.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(bgColor.copy(alpha = 0.6f))
                                    .then(
                                        Modifier.padding(
                                            start  = 12.dp,
                                            end    = 4.dp,
                                            top    = 10.dp,
                                            bottom = 10.dp
                                        )
                                    ),
                                verticalAlignment     = Alignment.Top,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    modifier              = Modifier.weight(1f),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment     = Alignment.Top
                                ) {
                                    Text(emoji, fontSize = 14.sp)
                                    Column {
                                        Text(
                                            ann.title,
                                            color      = Color.White,
                                            fontWeight = FontWeight.SemiBold,
                                            fontSize   = 13.sp
                                        )
                                        Text(
                                            ann.message,
                                            color      = Color.White.copy(alpha = 0.75f),
                                            fontSize   = 12.sp,
                                            lineHeight = 17.sp
                                        )
                                    }
                                }
                                IconButton(
                                    onClick  = { dismissed.add(ann.title) },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(
                                        Icons.Default.Close,
                                        contentDescription = "Dismiss",
                                        tint     = Color.White.copy(alpha = 0.5f),
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                            }
                        }
                    }
            }

            // ── Tab Content ───────────────────────────────────────
            AnimatedContent(
                targetState   = selectedTab,
                transitionSpec = { fadeIn() togetherWith fadeOut() },
                label         = "tab",
                modifier      = Modifier.fillMaxSize()
            ) { tab ->
                when (tab) {
                    0 -> TouchpadTab(viewModel)
                    1 -> RemoteDesktopScreen(
                        viewModel = viewModel,
                        userPrefs = userPrefs,
                        isPremium = isPremium,
                        onUpgrade = { showLicense = true },
                        onBack    = { selectedTab = 0 }
                    )
                    2 -> KeyboardTab(viewModel)
                    3 -> MediaTab(viewModel)
                }
            }
        }
    }
}
