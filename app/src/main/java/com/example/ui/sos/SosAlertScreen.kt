package com.example.ui.sos

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.MainViewModel

@Composable
fun SosAlertScreen(
    viewModel: MainViewModel
) {
    val alertState by viewModel.incomingSosAlert.collectAsState()

    if (!alertState.isTriggered) return

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF131313)) // dark background
            .padding(24.dp)
            .testTag("sos_alert_dialog_root"),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .border(3.dp, MaterialTheme.colorScheme.error)
                .background(Color(0xFF1C1B1B))
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Default.Warning,
                contentDescription = "Warning",
                tint = MaterialTheme.colorScheme.error,
                modifier = Modifier
                    .size(64.dp)
                    .padding(bottom = 12.dp)
            )

            Text(
                text = "EMERGENCY SOS RECEIVED!",
                fontSize = 20.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(bottom = 8.dp),
                textAlign = TextAlign.Center
            )

            Divider(color = MaterialTheme.colorScheme.error, thickness = 1.dp, modifier = Modifier.padding(bottom = 16.dp))

            // Sender Information
            Text(
                text = "SENDER",
                fontSize = 11.sp,
                fontFamily = FontFamily.Monospace,
                color = MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier.fillMaxWidth()
            )
            Text(
                text = alertState.senderName,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp)
                    .testTag("sos_sender_name")
            )

            // Message text
            Text(
                text = "EMERGENCY MESSAGE",
                fontSize = 11.sp,
                fontFamily = FontFamily.Monospace,
                color = MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier.fillMaxWidth()
            )
            Text(
                text = alertState.payload,
                fontSize = 15.sp,
                color = Color.White,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp)
                    .testTag("sos_message_text"),
                textAlign = TextAlign.Left
            )

            // GPS Location
            Text(
                text = "GPS LOCATION",
                fontSize = 11.sp,
                fontFamily = FontFamily.Monospace,
                color = MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier.fillMaxWidth()
            )
            val locationText = if (alertState.lat != 0.0 || alertState.lon != 0.0) {
                val ns = if (alertState.lat >= 0) "N" else "S"
                val ew = if (alertState.lon >= 0) "E" else "W"
                String.format("📍 %.4f°%s, %.4f°%s", Math.abs(alertState.lat), ns, Math.abs(alertState.lon), ew)
            } else {
                "Not Shared"
            }
            Text(
                text = locationText,
                fontSize = 14.sp,
                fontFamily = FontFamily.Monospace,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp)
                    .testTag("sos_location_text")
            )

            // Hop count latencies
            Text(
                text = "ESTIMATED HOPS",
                fontSize = 11.sp,
                fontFamily = FontFamily.Monospace,
                color = MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier.fillMaxWidth()
            )
            Text(
                text = "${alertState.estimatedHop} hop(s)",
                fontSize = 14.sp,
                fontFamily = FontFamily.Monospace,
                color = MaterialTheme.colorScheme.secondary,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 24.dp)
                    .testTag("sos_hop_count")
            )

            // Dismiss Button
            Button(
                onClick = { viewModel.dismissSosAlert() },
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error,
                    contentColor = MaterialTheme.colorScheme.onError
                ),
                shape = MaterialTheme.shapes.small,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .testTag("dismiss_sos_button")
            ) {
                Text(
                    text = "DISMISS SOS ALERT",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
            }
        }
    }
}
