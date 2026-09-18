package com.example.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.CloudQueue
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.sync.SyncStatus

private val OnlineGreen = Color(0xFF16A34A)
private val WarningAmber = Color(0xFFD97706)

@Composable
fun SyncStatusIndicator(
    syncStatus: SyncStatus,
    onManualSync: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showDialog by remember { mutableStateOf(false) }

    val infiniteTransition = rememberInfiniteTransition(label = "sync_rotation")
    val rotationAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotate_angle"
    )

    IconButton(
        onClick = { showDialog = true },
        modifier = modifier.testTag("top_bar_sync_indicator")
    ) {
        when (syncStatus) {
            is SyncStatus.OnlineSynced -> {
                Icon(
                    imageVector = Icons.Default.CloudDone,
                    contentDescription = "Cloud Synced",
                    tint = OnlineGreen,
                    modifier = Modifier.size(24.dp)
                )
            }
            is SyncStatus.Syncing -> {
                Icon(
                    imageVector = Icons.Default.CloudSync,
                    contentDescription = "Syncing with Cloud",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .size(24.dp)
                        .rotate(rotationAngle)
                )
            }
            is SyncStatus.OfflinePending -> {
                BadgedBox(
                    badge = {
                        Badge(
                            containerColor = WarningAmber,
                            contentColor = Color.White
                        ) {
                            Text(
                                text = "${syncStatus.pendingCount}",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                ) {
                    Icon(
                        imageVector = Icons.Default.CloudOff,
                        contentDescription = "Offline with pending changes",
                        tint = WarningAmber,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
            is SyncStatus.OfflineSynced -> {
                Icon(
                    imageVector = Icons.Default.CloudOff,
                    contentDescription = "Offline - Cached",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }

    if (showDialog) {
        SyncStatusDialog(
            syncStatus = syncStatus,
            onDismiss = { showDialog = false },
            onManualSync = {
                showDialog = false
                onManualSync()
            }
        )
    }
}

@Composable
fun SyncStatusDialog(
    syncStatus: SyncStatus,
    onDismiss: () -> Unit,
    onManualSync: () -> Unit
) {
    val isOnline = syncStatus is SyncStatus.OnlineSynced || syncStatus is SyncStatus.Syncing
    val pendingCount = when (syncStatus) {
        is SyncStatus.OfflinePending -> syncStatus.pendingCount
        is SyncStatus.Syncing -> syncStatus.pendingCount
        else -> 0
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = if (isOnline) Icons.Default.Wifi else Icons.Default.WifiOff,
                    contentDescription = null,
                    tint = if (isOnline) OnlineGreen else WarningAmber
                )
                Text(
                    text = "Cloud Sync Status",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Connection badge
                Surface(
                    color = if (isOnline) Color(0xFFDCFCE7) else Color(0xFFFEF3C7),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .background(
                                    if (isOnline) OnlineGreen else WarningAmber,
                                    CircleShape
                                )
                        )
                        Text(
                            text = if (isOnline) "Connected (Online)" else "Disconnected (Offline)",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = if (isOnline) Color(0xFF15803D) else Color(0xFFB45309)
                        )
                    }
                }

                // Description
                when (syncStatus) {
                    is SyncStatus.OnlineSynced -> {
                        Text(
                            text = "All sales and expenses are fully synced with Firebase Firestore in real-time. Both owners share the same live ledger data.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    is SyncStatus.Syncing -> {
                        Text(
                            text = "Synchronizing data with Firebase Firestore... $pendingCount pending changes uploading.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    is SyncStatus.OfflinePending -> {
                        Text(
                            text = "You are working offline. $pendingCount local changes are safely queued in your local Room database. They will automatically sync to Firestore as soon as internet connectivity is restored.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    is SyncStatus.OfflineSynced -> {
                        Text(
                            text = "You are currently offline. All local sales and expenses are cached and up to date.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Storage: Firebase Firestore (Source of Truth) + Room (Offline Cache)",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                )
            }
        },
        confirmButton = {
            if (isOnline) {
                Button(
                    onClick = onManualSync,
                    modifier = Modifier.testTag("btn_dialog_sync_now")
                ) {
                    Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Sync Now")
                }
            } else {
                Button(
                    onClick = onDismiss,
                    modifier = Modifier.testTag("btn_dialog_ok")
                ) {
                    Text("OK")
                }
            }
        },
        dismissButton = {
            if (isOnline) {
                OutlinedButton(onClick = onDismiss) {
                    Text("Close")
                }
            }
        }
    )
}
