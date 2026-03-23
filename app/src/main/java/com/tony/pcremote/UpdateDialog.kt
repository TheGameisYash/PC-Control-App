package com.tony.pcremote

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

@Composable
fun UpdateDialog(
    info: UpdateInfo,
    onDismiss: () -> Unit
) {
    val context  = LocalContext.current
    val scope    = rememberCoroutineScope()
    var progress by remember { mutableIntStateOf(-1) }
    var isDone   by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = { if (progress == -1) onDismiss() },
        title = {
            Text("Update Available — v${info.version_name}")
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(info.changelog)

                if (progress in 0..99) {
                    Spacer(modifier = Modifier.height(8.dp))
                    LinearProgressIndicator(
                        progress = { progress / 100f },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Text("Downloading... $progress%")
                }

                if (isDone) {
                    Text("✅ Download complete. Installing...")
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    scope.launch {
                        val file = UpdateManager.downloadApk(
                            context  = context,
                            apkUrl   = info.apk_url,
                            onProgress = { progress = it }
                        )
                        if (file != null) {
                            isDone = true
                            UpdateManager.installApk(context, file)
                        }
                        onDismiss()
                    }
                },
                enabled = progress == -1
            ) {
                Text("Update Now")
            }
        },
        dismissButton = {
            TextButton(
                onClick  = onDismiss,
                enabled  = progress == -1
            ) {
                Text("Later")
            }
        }
    )
}
