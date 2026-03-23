package com.tony.pcremote

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

data class UtilityItem(
    val title: String,
    val desc: String,
    val icon: ImageVector,
    val color: Color,
    val type: UtilityType
)

enum class UtilityType {
    REMOTE_DESKTOP, FILE_TRANSFER, TASK_MANAGER, WEBCAM, TERMINAL, VOLUME_MIXER
}

@Composable
fun UtilitiesTab(
    viewModel: MainViewModel,
    onNavigateToDesktop: () -> Unit,
    onUtilitySelected: (UtilityType) -> Unit
) {
    val items = listOf(
        UtilityItem("Remote Desktop", "View and control PC screen", Icons.Rounded.DesktopWindows, Color(0xFF00E5FF), UtilityType.REMOTE_DESKTOP),
        UtilityItem("File Transfer", "Manage your PC files", Icons.Rounded.FolderCopy, Color(0xFFFF4081), UtilityType.FILE_TRANSFER),
        UtilityItem("Task Manager", "Monitor PC performance", Icons.Rounded.Assessment, Color(0xFF7C4DFF), UtilityType.TASK_MANAGER),
        UtilityItem("Webcam", "Use phone as PC camera", Icons.Rounded.Videocam, Color(0xFF00C853), UtilityType.WEBCAM),
        UtilityItem("Terminal", "Execute remote commands", Icons.Rounded.Terminal, Color(0xFFFFAB40), UtilityType.TERMINAL),
        UtilityItem("Volume Mixer", "Control app volumes", Icons.Rounded.Tune, Color(0xFF448AFF), UtilityType.VOLUME_MIXER)
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        // Feature Banner
        Surface(
            modifier = Modifier.fillMaxWidth().height(120.dp),
            shape = RoundedCornerShape(28.dp),
            color = Color(0xFF121212),
            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.05f))
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                Box(modifier = Modifier.fillMaxSize().background(Brush.linearGradient(listOf(Color(0xFF00E5FF).copy(alpha = 0.05f), Color.Transparent))))
                Row(modifier = Modifier.padding(24.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Smart Tools", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Black)
                        Text("Powerful utilities for your PC", color = Color.White.copy(alpha = 0.4f), fontSize = 12.sp)
                    }
                    Icon(Icons.Rounded.AutoAwesome, null, tint = Color(0xFF00E5FF), modifier = Modifier.size(40.dp))
                }
            }
        }

        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 20.dp)
        ) {
            items(items) { item ->
                UtilityCard(item) { 
                    if (item.type == UtilityType.REMOTE_DESKTOP) onNavigateToDesktop()
                    else onUtilitySelected(item.type)
                }
            }
        }
    }
}

@Composable
fun UtilityCard(item: UtilityItem, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().height(150.dp),
        shape = RoundedCornerShape(24.dp),
        color = Color(0xFF121212),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.05f))
    ) {
        Column(modifier = Modifier.fillMaxSize().padding(20.dp), verticalArrangement = Arrangement.SpaceBetween) {
            Box(modifier = Modifier.size(44.dp).clip(CircleShape).background(item.color.copy(alpha = 0.1f)), contentAlignment = Alignment.Center) {
                Icon(item.icon, null, tint = item.color, modifier = Modifier.size(22.dp))
            }
            Column {
                Text(item.title, color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold, letterSpacing = (-0.3).sp)
                Spacer(Modifier.height(4.dp))
                Text(item.desc, color = Color.White.copy(alpha = 0.3f), fontSize = 10.sp, lineHeight = 14.sp)
            }
        }
    }
}
