package com.f1telemetry.viewer

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Leaderboard
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.f1telemetry.viewer.data.MainViewModel
import com.f1telemetry.viewer.ui.screens.AnalysisScreen
import com.f1telemetry.viewer.ui.screens.CarScreen
import com.f1telemetry.viewer.ui.screens.DashboardScreen
import com.f1telemetry.viewer.ui.screens.RaceScreen
import com.f1telemetry.viewer.ui.screens.ReplayScreen
import com.f1telemetry.viewer.ui.screens.TracesScreen
import com.f1telemetry.viewer.ui.theme.AccentGreen
import com.f1telemetry.viewer.ui.theme.F1Dark
import com.f1telemetry.viewer.ui.theme.F1Red
import com.f1telemetry.viewer.ui.theme.F1Surface
import com.f1telemetry.viewer.ui.theme.F1Theme
import com.f1telemetry.viewer.ui.theme.TextDim

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent { F1Theme(useDark = true) { AppRoot() } }
    }
}

private data class Tab(val label: String, val icon: ImageVector)

@Composable
private fun AppRoot(vm: MainViewModel = viewModel()) {
    val telemetry by vm.telemetry.collectAsStateWithLifecycle()
    val controller by vm.controller.collectAsStateWithLifecycle()
    var selected by remember { mutableIntStateOf(0) }

    val tabs = listOf(
        Tab("Dash", Icons.Filled.Speed),
        Tab("Race", Icons.Filled.Leaderboard),
        Tab("Traces", Icons.Filled.Info),
        Tab("Car", Icons.Filled.Build),
        Tab("Analysis", Icons.Filled.Settings),
        Tab("Connect", Icons.Filled.PlayArrow),
    )

    Scaffold(
        containerColor = F1Dark,
        topBar = { TopHeader(telemetry.connected, telemetry.playerName, controller.recording) },
        bottomBar = {
            NavigationBar(containerColor = F1Surface) {
                tabs.forEachIndexed { i, tab ->
                    NavigationBarItem(
                        selected = selected == i,
                        onClick = { selected = i },
                        icon = { Icon(tab.icon, contentDescription = tab.label) },
                        label = { Text(tab.label, fontSize = 10.sp) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = F1Red,
                            selectedTextColor = F1Red,
                            indicatorColor = F1Dark,
                            unselectedIconColor = TextDim,
                            unselectedTextColor = TextDim,
                        ),
                    )
                }
            }
        },
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            when (selected) {
                0 -> DashboardScreen(telemetry)
                1 -> RaceScreen(telemetry)
                2 -> TracesScreen(telemetry)
                3 -> CarScreen(telemetry)
                4 -> AnalysisScreen(telemetry)
                else -> ReplayScreen(
                    controller = controller,
                    telemetry = telemetry,
                    onStartLive = vm::startLive,
                    onStopLive = vm::stopLive,
                    onToggleRecording = vm::toggleRecording,
                    onSetPort = vm::setPort,
                    onPlay = vm::playRecording,
                    onStopReplay = vm::stopReplay,
                    onDelete = vm::deleteRecording,
                    onSetSpeed = vm::setReplaySpeed,
                    onRefresh = vm::refreshRecordings,
                )
            }
        }
    }
}

@Composable
private fun TopHeader(connected: Boolean, player: String, recording: Boolean) {
    Row(
        Modifier.fillMaxWidth().height(52.dp).padding(horizontal = 16.dp),
        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
    ) {
        Text("F1 ", color = F1Red, fontSize = 20.sp, fontWeight = FontWeight.Black)
        Text("TELEMETRY", color = MaterialTheme.colorScheme.onBackground, fontSize = 20.sp, fontWeight = FontWeight.Light)
        Spacer(Modifier.weight(1f))
        if (recording) {
            Text("● REC  ", color = F1Red, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        }
        Text(
            if (connected) "LIVE" else "OFFLINE",
            color = if (connected) AccentGreen else TextDim,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
        )
    }
}
