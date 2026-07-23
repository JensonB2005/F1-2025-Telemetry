package com.f1telemetry.viewer.ui.screens

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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.f1telemetry.viewer.data.ControllerState
import com.f1telemetry.viewer.data.Mode
import com.f1telemetry.viewer.data.TelemetryState
import java.net.Inet4Address
import java.net.NetworkInterface
import com.f1telemetry.viewer.ui.Fmt
import com.f1telemetry.viewer.ui.components.LabeledValue
import com.f1telemetry.viewer.ui.components.SectionCard
import com.f1telemetry.viewer.ui.theme.AccentCyan
import com.f1telemetry.viewer.ui.theme.AccentGreen
import com.f1telemetry.viewer.ui.theme.F1Red
import com.f1telemetry.viewer.ui.theme.TextDim

@Composable
fun ReplayScreen(
    controller: ControllerState,
    telemetry: TelemetryState,
    onStartLive: () -> Unit,
    onStopLive: () -> Unit,
    onToggleRecording: () -> Unit,
    onSetPort: (Int) -> Unit,
    onPlay: (String, Float) -> Unit,
    onStopReplay: () -> Unit,
    onDelete: (String) -> Unit,
    onSetSpeed: (Float) -> Unit,
    onRefresh: () -> Unit,
    onSelfTest: () -> Unit,
) {
    var portText by remember(controller.port) { mutableStateOf(controller.port.toString()) }
    var speed by remember { mutableStateOf(1f) }

    Column(
        Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        SectionCard("Live connection") {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier.size(12.dp).clip(RoundedCornerShape(6.dp))
                        .background(if (telemetry.connected) AccentGreen else F1Red),
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    if (telemetry.connected) "Receiving — ${telemetry.source}" else "Not receiving",
                    color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.SemiBold,
                )
            }
            Spacer(Modifier.height(6.dp))
            Text(
                "Datagrams: ${telemetry.datagramsReceived}   •   parsed: ${telemetry.packetsReceived}   •   format ${telemetry.packetFormat}   •   game '${telemetry.gameYear}",
                color = TextDim, fontSize = 11.sp,
            )
            if (telemetry.lastSenderIp.isNotEmpty()) {
                Text("Last sender: ${telemetry.lastSenderIp}", color = TextDim, fontSize = 11.sp)
            }
            Spacer(Modifier.height(6.dp))
            Text(
                "This device's IP: ${localIpAddress() ?: "unknown"}",
                color = AccentCyan, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, fontFamily = FontFamily.Monospace,
            )
            Text("Enter this IP in the game (or use Broadcast Mode).", color = TextDim, fontSize = 11.sp)
            Spacer(Modifier.height(10.dp))
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = portText,
                    onValueChange = {
                        portText = it.filter { c -> c.isDigit() }.take(5)
                        portText.toIntOrNull()?.let { p -> onSetPort(p) }
                    },
                    label = { Text("UDP port") },
                    singleLine = true,
                    modifier = Modifier.width(120.dp),
                )
                if (controller.mode == Mode.LIVE) {
                    Button(onClick = onStopLive, colors = ButtonDefaults.buttonColors(containerColor = F1Red)) { Text("Stop") }
                } else {
                    Button(onClick = onStartLive, colors = ButtonDefaults.buttonColors(containerColor = AccentGreen)) { Text("Start listening") }
                }
                OutlinedButton(onClick = onSelfTest) { Text("Self-test") }
            }
            if (controller.mode == Mode.LIVE) {
                Text("Socket listening on 0.0.0.0:${controller.port}", color = AccentGreen, fontSize = 11.sp)
            }
            Text(
                "Self-test sends a packet to this app. If 'Datagrams' goes up after tapping it, the app is " +
                    "receiving fine and the issue is the game's IP/port or the network — not the app.",
                color = TextDim, fontSize = 11.sp,
            )
            controller.error?.let {
                Spacer(Modifier.height(6.dp))
                Text("Error: $it", color = F1Red, fontSize = 12.sp)
            }
            Spacer(Modifier.height(8.dp))
            Text("Setup & troubleshooting", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            Text(
                "1. F1 25 → Settings → Telemetry Settings → UDP Telemetry: ON.\n" +
                    "2. UDP IP Address: the device IP shown above (or turn UDP Broadcast Mode ON to skip the IP).\n" +
                    "3. UDP Port: ${controller.port}   •   UDP Format: 2025   •   Send Rate: 20–60 Hz.\n" +
                    "4. Phone and PC/console must be on the SAME Wi-Fi/LAN.\n\n" +
                    "If 'Datagrams' stays 0: the packets aren't reaching the phone — check the IP, that both are on the same network, and that the router isn't blocking device-to-device traffic (AP/client isolation, or guest Wi-Fi). Some PC firewalls block outgoing UDP.\n" +
                    "If 'Datagrams' climbs but 'parsed' stays 0: set the game's UDP Format to 2025.",
                color = TextDim, fontSize = 11.sp,
            )
        }

        SectionCard("Recording") {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (controller.recording) {
                    Button(onClick = onToggleRecording, colors = ButtonDefaults.buttonColors(containerColor = F1Red)) { Text("Stop recording") }
                    Spacer(Modifier.width(10.dp))
                    Text("● REC ${controller.recordingName}", color = F1Red, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                } else {
                    Button(
                        onClick = onToggleRecording,
                        enabled = controller.mode == Mode.LIVE,
                        colors = ButtonDefaults.buttonColors(containerColor = AccentCyan),
                    ) { Text("Record session") }
                    Spacer(Modifier.width(10.dp))
                    Text(if (controller.mode == Mode.LIVE) "Ready" else "Start live first", color = TextDim, fontSize = 12.sp)
                }
            }
        }

        if (controller.mode == Mode.REPLAY) {
            SectionCard("Now replaying") {
                LabeledValue("File", controller.replay.fileName ?: "—", AccentCyan)
                LabeledValue("Packet", "${controller.replay.packetIndex} / ${controller.replay.totalPackets}")
                LabeledValue("Position", "${controller.replay.offsetMs / 1000}s / ${controller.replay.durationMs / 1000}s")
                Spacer(Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(0.5f, 1f, 2f, 4f).forEach { sp ->
                        OutlinedButton(onClick = { onSetSpeed(sp); speed = sp }) { Text("${sp}x") }
                    }
                    Button(onClick = onStopReplay, colors = ButtonDefaults.buttonColors(containerColor = F1Red)) { Text("Stop") }
                }
            }
        }

        SectionCard("Saved recordings") {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("${controller.recordings.size} session(s)", color = TextDim, fontSize = 12.sp)
                OutlinedButton(onClick = onRefresh) { Text("Refresh") }
            }
            Spacer(Modifier.height(6.dp))
            if (controller.recordings.isEmpty()) {
                Text("No recordings yet. Start live telemetry and tap Record.", color = TextDim, fontSize = 13.sp)
            } else {
                controller.recordings.forEach { rec ->
                    Row(
                        Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(rec.name, color = Color.White, fontSize = 13.sp, fontFamily = FontFamily.Monospace)
                            Text(Fmt.size(rec.sizeBytes), color = TextDim, fontSize = 11.sp)
                        }
                        IconButton(onClick = { onPlay(rec.path, speed) }) {
                            Icon(Icons.Filled.PlayArrow, contentDescription = "Play", tint = AccentGreen)
                        }
                        IconButton(onClick = { onDelete(rec.path) }) {
                            Icon(Icons.Filled.Delete, contentDescription = "Delete", tint = F1Red)
                        }
                    }
                }
            }
        }
    }
}

/** Best-guess LAN IPv4 of this device, to show the user what to enter in the game. */
private fun localIpAddress(): String? {
    return try {
        java.util.Collections.list(NetworkInterface.getNetworkInterfaces())
            .filter { it.isUp && !it.isLoopback }
            .flatMap { java.util.Collections.list(it.inetAddresses) }
            .filterIsInstance<Inet4Address>()
            .firstOrNull { it.isSiteLocalAddress }
            ?.hostAddress
    } catch (e: Exception) {
        null
    }
}
