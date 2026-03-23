package com.tony.pcremote

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskManagerScreen(viewModel: MainViewModel, onBack: () -> Unit) {
    val processes by viewModel.processes.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.refreshProcesses()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Task Manager", color = Color.White) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, null, tint = Color.White)
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.refreshProcesses() }) {
                        Icon(Icons.Rounded.Refresh, null, tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF050505))
            )
        },
        containerColor = Color(0xFF050505)
    ) { padding ->
        LazyColumn(modifier = Modifier.padding(padding)) {
            items(processes.sortedByDescending { it.cpu }) { proc ->
                ListItem(
                    headlineContent = { Text(proc.name, color = Color.White, fontWeight = FontWeight.Bold) },
                    supportingContent = { 
                        Text("PID: ${proc.pid} | CPU: ${String.format("%.1f", proc.cpu)}% | Mem: ${proc.memory / 1024 / 1024}MB", 
                        color = Color.White.copy(alpha = 0.5f)) 
                    },
                    trailingContent = {
                        IconButton(onClick = { viewModel.killProcess(proc.pid) }) {
                            Icon(Icons.Rounded.Close, null, tint = Color(0xFFFF5252))
                        }
                    },
                    colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                )
                HorizontalDivider(color = Color.White.copy(alpha = 0.05f))
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FileExplorerScreen(viewModel: MainViewModel, onBack: () -> Unit) {
    val files by viewModel.files.collectAsState()
    val currentPath by viewModel.currentFilePath.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.requestFiles("C:\\") // Initial path
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("File Transfer", color = Color.White) },
                navigationIcon = {
                    IconButton(onClick = {
                        if (currentPath == "C:\\") onBack()
                        else viewModel.navigateUp()
                    }) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, null, tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF050505))
            )
        },
        containerColor = Color(0xFF050505)
    ) { padding ->
        Column(modifier = Modifier.padding(padding)) {
            Text(currentPath, color = Color(0xFF00E5FF), fontSize = 12.sp, modifier = Modifier.padding(16.dp))
            LazyColumn {
                items(files) { file ->
                    ListItem(
                        modifier = Modifier.clickable { 
                            if (file.isDirectory) viewModel.requestFiles(file.path)
                        },
                        headlineContent = { Text(file.name, color = Color.White) },
                        leadingContent = {
                            Icon(
                                if (file.isDirectory) Icons.Rounded.Folder else Icons.Rounded.Description,
                                null,
                                tint = if (file.isDirectory) Color(0xFFFFAB40) else Color.White.copy(alpha = 0.5f)
                            )
                        },
                        colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VolumeMixerScreen(viewModel: MainViewModel, onBack: () -> Unit) {
    val devices by viewModel.volumeDevices.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.refreshVolumes()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Volume Mixer", color = Color.White) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, null, tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF050505))
            )
        },
        containerColor = Color(0xFF050505)
    ) { padding ->
        LazyColumn(modifier = Modifier.padding(padding).padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            items(devices) { device ->
                Column {
                    Text(device.name, color = Color.White, fontSize = 14.sp)
                    Slider(
                        value = device.level.toFloat(),
                        onValueChange = { viewModel.setVolume(device.id, it.toInt()) },
                        valueRange = 0f..100f,
                        colors = SliderDefaults.colors(thumbColor = Color(0xFF448AFF), activeTrackColor = Color(0xFF448AFF))
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TerminalScreen(viewModel: MainViewModel, onBack: () -> Unit) {
    var command by remember { mutableStateOf("") }
    val output by viewModel.terminalOutput.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Terminal", color = Color.White) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, null, tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF050505))
            )
        },
        containerColor = Color(0xFF050505)
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize().padding(16.dp)) {
            Box(modifier = Modifier.weight(1f).fillMaxWidth().background(Color(0xFF0A0A0A), RoundedCornerShape(8.dp)).padding(8.dp)) {
                Text(output, color = Color(0xFF00FF00), fontSize = 12.sp)
            }
            Spacer(Modifier.height(16.dp))
            OutlinedTextField(
                value = command,
                onValueChange = { command = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Enter command...", color = Color.White.copy(alpha = 0.3f)) },
                trailingIcon = {
                    IconButton(onClick = { 
                        if (command.isNotBlank()) {
                            viewModel.runTerminalCommand(command)
                            command = ""
                        }
                    }) {
                        Icon(Icons.Rounded.Send, null, tint = Color(0xFF00E5FF))
                    }
                },
                colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
            )
        }
    }
}
