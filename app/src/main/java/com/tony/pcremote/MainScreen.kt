package com.tony.pcremote

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerDefaults
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch

private data class NavTab(
    val icon: ImageVector,
    val label: String,
    val activeColor: Color
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    viewModel:    MainViewModel,
    userPrefs:    UserPreferences,
    onDisconnect: () -> Unit
) {
    val isPremium     by userPrefs.isPremium.collectAsState(initial = false)
    val pcName        by viewModel.pcName.collectAsState()
    val isConnected   by viewModel.isConnected.collectAsState()

    val navTabs = remember {
        listOf(
            NavTab(Icons.Rounded.Mouse, "Remote", Color(0xFF00E5FF)),
            NavTab(Icons.Rounded.Keyboard, "Keyboard", Color(0xFF7C4DFF)),
            NavTab(Icons.Rounded.GridView, "Utils", Color(0xFFFF4081)),
            NavTab(Icons.Rounded.Router, "Connect", Color(0xFF00C853)),
            NavTab(Icons.Rounded.Person, "Account", Color(0xFFFFAB40))
        )
    }

    val pagerState = rememberPagerState(pageCount = { navTabs.size })
    val scope = rememberCoroutineScope()
    var showLicense  by remember { mutableStateOf(false) }
    var activeUtility by remember { mutableStateOf<UtilityType?>(null) }

    LaunchedEffect(Unit) {
        viewModel.fetchAnnouncements()
    }

    if (showLicense) {
        LicenseScreen(
            userPrefs   = userPrefs,
            onActivated = { showLicense = false },
            onBack      = { showLicense = false }
        )
        return
    }

    // Handle Utility Screens Overlay
    if (activeUtility != null) {
        when (activeUtility) {
            UtilityType.REMOTE_DESKTOP -> RemoteDesktopScreen(
                viewModel = viewModel,
                userPrefs = userPrefs,
                isPremium = isPremium,
                onUpgrade = { showLicense = true },
                onBack = { activeUtility = null }
            )
            UtilityType.TASK_MANAGER -> TaskManagerScreen(viewModel, onBack = { activeUtility = null })
            UtilityType.FILE_TRANSFER -> FileExplorerScreen(viewModel, onBack = { activeUtility = null })
            UtilityType.VOLUME_MIXER -> VolumeMixerScreen(viewModel, onBack = { activeUtility = null })
            UtilityType.TERMINAL -> TerminalScreen(viewModel, onBack = { activeUtility = null })
            UtilityType.WEBCAM -> { /* Webcam placeholder */ activeUtility = null }
            else -> activeUtility = null
        }
        return
    }

    // Smoothly animate the background glow color based on pager progress
    val accentColor by animateColorAsState(
        targetValue = navTabs[pagerState.currentPage].activeColor,
        animationSpec = tween(400),
        label = "AccentColor"
    )

    Scaffold(
        containerColor = Color(0xFF050505),
        bottomBar = {
            ModernNavigationBar(
                tabs = navTabs,
                selectedTab = pagerState.currentPage,
                onTabSelected = { index ->
                    scope.launch { 
                        pagerState.animateScrollToPage(
                            index, 
                            animationSpec = spring(stiffness = Spring.StiffnessLow)
                        ) 
                    }
                }
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Static Base Background
            Box(modifier = Modifier.fillMaxSize().background(Color(0xFF050505)))
            
            // Dynamic Accent Glow
            Box(
                modifier = Modifier
                    .size(500.dp)
                    .align(Alignment.TopCenter)
                    .offset(y = (-250).dp)
                    .graphicsLayer { alpha = 0.4f }
                    .background(Brush.radialGradient(listOf(accentColor.copy(alpha = 0.2f), Color.Transparent)))
            )

            Column(modifier = Modifier.fillMaxSize()) {
                HeaderSection(
                    title = navTabs[pagerState.currentPage].label,
                    isConnected = isConnected,
                    pcName = pcName,
                    accentColor = accentColor,
                    onAccountClick = { scope.launch { pagerState.animateScrollToPage(4) } }
                )

                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier.weight(1f),
                    beyondViewportPageCount = 1,
                    userScrollEnabled = true,
                    flingBehavior = PagerDefaults.flingBehavior(state = pagerState),
                    pageSpacing = 16.dp
                ) { page ->
                    Box(modifier = Modifier.fillMaxSize()) {
                        when (page) {
                            0 -> TouchpadTab(viewModel)
                            1 -> KeyboardTab(viewModel)
                            2 -> UtilitiesTab(
                                    viewModel = viewModel,
                                    onNavigateToDesktop = { activeUtility = UtilityType.REMOTE_DESKTOP },
                                    onUtilitySelected = { activeUtility = it }
                                )
                            3 -> ConnectScreen(
                                    viewModel = viewModel,
                                    onConnected = { scope.launch { pagerState.animateScrollToPage(0) } },
                                    onOpenLogin = { scope.launch { pagerState.animateScrollToPage(4) } }
                                )
                            4 -> AccountTab(
                                    userPrefs = userPrefs,
                                    viewModel = viewModel,
                                    onUpgrade = { showLicense = true },
                                    onDisconnect = onDisconnect
                                )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun HeaderSection(
    title: String,
    isConnected: Boolean,
    pcName: String,
    accentColor: Color,
    onAccountClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(horizontal = 24.dp, vertical = 20.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column {
            Text(
                text = title,
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontWeight = FontWeight.Black,
                    color = Color.White,
                    letterSpacing = (-1.5).sp
                )
            )
            if (isConnected) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 2.dp)) {
                    Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(Color(0xFF00E5FF)))
                    Spacer(Modifier.width(8.dp))
                    Text(
                        pcName, 
                        color = Color.White.copy(alpha = 0.5f), 
                        fontSize = 12.sp, 
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp
                    )
                }
            }
        }
        
        Surface(
            onClick = onAccountClick,
            shape = CircleShape,
            color = Color(0xFF1A1A1A),
            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.1f))
        ) {
            Icon(
                Icons.Rounded.Person, 
                null, 
                tint = accentColor,
                modifier = Modifier.padding(10.dp).size(24.dp)
            )
        }
    }
}

@Composable
private fun ModernNavigationBar(
    tabs: List<NavTab>,
    selectedTab: Int,
    onTabSelected: (Int) -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 20.dp, vertical = 16.dp),
        shape = RoundedCornerShape(32.dp),
        color = Color(0xFF121212).copy(alpha = 0.98f),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.05f)),
        tonalElevation = 16.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp, horizontal = 16.dp),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically
        ) {
            tabs.forEachIndexed { index, tab ->
                val isSelected = selectedTab == index
                
                Box(
                    modifier = Modifier
                        .clip(CircleShape)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) { onTabSelected(index) }
                        .padding(8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = tab.icon,
                            contentDescription = tab.label,
                            tint = if (isSelected) tab.activeColor else Color.White.copy(alpha = 0.4f),
                            modifier = Modifier.size(26.dp)
                        )
                        
                        val dotScale by animateFloatAsState(
                            targetValue = if (isSelected) 1f else 0f,
                            animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
                            label = "DotScale"
                        )
                        
                        Box(
                            modifier = Modifier
                                .padding(top = 6.dp)
                                .size(4.dp)
                                .graphicsLayer { 
                                    scaleX = dotScale
                                    scaleY = dotScale
                                    alpha = dotScale
                                }
                                .clip(CircleShape)
                                .background(tab.activeColor)
                        )
                    }
                }
            }
        }
    }
}
