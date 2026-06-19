package com.example.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.MainViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: MainViewModel,
    onBack: () -> Unit
) {
    val scrollState = rememberScrollState()

    val deviceIdState by viewModel.deviceId.collectAsState()
    val displayNameState by viewModel.displayName.collectAsState()
    val autoShareLocationState by viewModel.autoShareLocation.collectAsState()
    val playSoundOnSosState by viewModel.playSoundOnSos.collectAsState()
    val vibrateOnIncomingState by viewModel.vibrateOnIncoming.collectAsState()
    val isServiceRunningState by viewModel.isServiceRunning.collectAsState()
    val peersState by viewModel.connectedPeers.collectAsState()

    var nicknameInput by remember { mutableStateOf("") }

    LaunchedEffect(displayNameState) {
        nicknameInput = displayNameState
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "SYSTEM SETTINGS",
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = MaterialTheme.colorScheme.primary
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Back",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                ),
                modifier = Modifier.border(1.dp, MaterialTheme.colorScheme.outline)
            )
        },
        bottomBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .border(width = 1.dp, color = MaterialTheme.colorScheme.outlineVariant)
                    .navigationBarsPadding(),
                horizontalArrangement = Arrangement.SpaceAround,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Tab 1: Messages
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .weight(1f)
                        .clickable { onBack() }
                        .padding(vertical = 4.dp)
                ) {
                    Box(
                        modifier = Modifier.padding(vertical = 4.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("💬", fontSize = 16.sp, modifier = Modifier.alpha(0.6f))
                    }
                    Text(
                        text = "Messages",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    )
                }

                // Tab 2: Peers
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .weight(1f)
                        .clickable { onBack() }
                        .padding(vertical = 4.dp)
                ) {
                    Box(
                        modifier = Modifier.padding(vertical = 4.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("👥", fontSize = 16.sp, modifier = Modifier.alpha(0.6f))
                    }
                    Text(
                        text = "Peers",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    )
                }

                // Tab 3: Settings (Active)
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .weight(1f)
                        .clickable { /* Already on settings */ }
                        .padding(vertical = 4.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .background(MaterialTheme.colorScheme.primaryContainer, RoundedCornerShape(12.dp))
                            .padding(horizontal = 16.dp, vertical = 4.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("⚙️", fontSize = 16.sp)
                    }
                    Text(
                        text = "Settings",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
        },
        modifier = Modifier.testTag("settings_screen_root")
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(innerPadding)
                .padding(16.dp)
                .verticalScroll(scrollState),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Section 1: User Identity Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(2.dp, MaterialTheme.colorScheme.outline),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "IDENTITY",
                        fontSize = 12.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )

                    OutlinedTextField(
                        value = nicknameInput,
                        onValueChange = { nicknameInput = it.take(24) },
                        label = { Text("Display Name") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("settings_nickname_input")
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Button(
                        onClick = { viewModel.updateDisplayName(nicknameInput) },
                        modifier = Modifier
                            .align(Alignment.End)
                            .testTag("save_settings_button"),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer,
                            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                        ),
                        shape = MaterialTheme.shapes.small
                    ) {
                        Text("SAVE NAME", fontWeight = FontWeight.Bold)
                    }
                }
            }

            // Section 2: Service controls & debug status
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(2.dp, MaterialTheme.colorScheme.outline),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "MESH SERVICE STATUS",
                        fontSize = 12.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (isServiceRunningState) "● ACTIVE" else "○ INACTIVE",
                            fontSize = 14.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            color = if (isServiceRunningState) MaterialTheme.colorScheme.secondary else Color.Gray
                        )

                        Button(
                            onClick = {
                                if (isServiceRunningState) viewModel.stopService() else viewModel.startService()
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isServiceRunningState) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.secondary
                            ),
                            shape = MaterialTheme.shapes.small
                        ) {
                            Text(
                                if (isServiceRunningState) "STOP MESH" else "START MESH",
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            // Section 3: Preferences/Toggles Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(2.dp, MaterialTheme.colorScheme.outline),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "PREFERENCES",
                        fontSize = 12.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )

                    // Autoshare Location toggle
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = LayoutArrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Auto-Share GPS Location", color = MaterialTheme.colorScheme.onSurface)
                        Switch(
                            checked = autoShareLocationState,
                            onCheckedChange = { viewModel.updateAutoShareLocation(it) }
                        )
                    }

                    // Play Sound on incoming SOS toggle
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = LayoutArrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Sound Alarm on Incoming SOS", color = MaterialTheme.colorScheme.onSurface)
                        Switch(
                            checked = playSoundOnSosState,
                            onCheckedChange = { viewModel.updatePlaySoundOnSos(it) }
                        )
                    }

                    // Vibrate on incoming message toggle
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = LayoutArrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Vibrate on Message Broadcast", color = MaterialTheme.colorScheme.onSurface)
                        Switch(
                            checked = vibrateOnIncomingState,
                            onCheckedChange = { viewModel.updateVibrateOnIncoming(it) }
                        )
                    }
                }
            }

            // Section 4: Device metadata
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(2.dp, MaterialTheme.colorScheme.outline),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "METADATA",
                        fontSize = 12.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )

                    // Device ID (last 8 chars)
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("My Device ID", color = Color.Gray, fontSize = 14.sp)
                        Text(
                            text = deviceIdState.takeLast(8).uppercase(),
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    }

                    // App Version
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("App Version", color = Color.Gray, fontSize = 14.sp)
                        Text(
                            text = "1.0",
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    }
                }
            }

            // Section 5: Debug Peer logs Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(2.dp, MaterialTheme.colorScheme.outline),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "DEBUG DISCOVERY LOG",
                        fontSize = 12.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )

                    if (peersState.isEmpty()) {
                        Text(
                            text = "No peers currently connected. Move closer or scanning...",
                            color = Color.Gray,
                            fontSize = 13.sp
                        )
                    } else {
                        peersState.forEach { peer ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(peer.displayName, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                Text(
                                    "Endpoint: ${peer.endpointId.takeLast(6).uppercase()}",
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 12.sp,
                                    color = Color.Gray
                                )
                            }
                        }
                    }
                }
            }

            // Button 6: Clear History
            Button(
                onClick = { viewModel.clearHistory() },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("clear_history_button"),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error,
                    contentColor = MaterialTheme.colorScheme.onError
                ),
                shape = MaterialTheme.shapes.small
            ) {
                Text("CLEAR MESSAGE HISTORY", fontWeight = FontWeight.Bold)
            }
        }
    }
}

// Help resolve layout references for compose compile mapping
private object LayoutArrangement {
    val SpaceBetween = Arrangement.SpaceBetween
}

private object AlignmentResolver {
    val CenterVertically = Alignment.CenterVertically
}
